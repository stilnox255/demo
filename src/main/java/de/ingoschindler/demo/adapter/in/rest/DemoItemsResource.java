package de.ingoschindler.demo.adapter.in.rest;

import de.ingoschindler.demo.adapter.in.rest.dto.DemoItemMapper;
import de.ingoschindler.demo.adapter.in.rest.dto.DemoItemRequest;
import de.ingoschindler.demo.adapter.in.rest.dto.DemoItemResponse;
import de.ingoschindler.demo.application.port.in.ArchiveStaleDemoItemsUseCase;
import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemCommand;
import de.ingoschindler.demo.application.port.in.AttachFileToDemoItemUseCase;
import de.ingoschindler.demo.application.port.in.CreateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.CreateDemoItemUseCase;
import de.ingoschindler.demo.application.port.in.DeleteDemoItemUseCase;
import de.ingoschindler.demo.application.port.in.DemoItemQueryPort;
import de.ingoschindler.demo.application.port.in.DownloadDemoItemAttachmentUseCase;
import de.ingoschindler.demo.application.port.in.UpdateDemoItemCommand;
import de.ingoschindler.demo.application.port.in.UpdateDemoItemUseCase;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemStatus;
import de.ingoschindler.kernel.download.DownloadTokenPort;
import de.ingoschindler.kernel.pagination.PageRequest;
import de.ingoschindler.kernel.pagination.PagedResult;
import de.ingoschindler.kernel.upload.UploadedFiles;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.util.UUID;

/**
 * HTTP surface of the demo BC.
 *
 * <p>Resource paths are plural nouns with the id in the path and sub-resources
 * below it (ADR-05); there is no {@code /api/v1} prefix (ADR-07). Every endpoint
 * carries OpenAPI annotations, because a generated spec with no descriptions is a
 * type listing, not documentation (ADR-11).</p>
 *
 * <p>The resource does three things and no more: translate the request, call one
 * in-port, translate the response. It never touches a repository, and it depends
 * on {@code *UseCase} / {@code *QueryPort} interfaces rather than on the
 * implementing classes.</p>
 *
 * <p>{@code ownerId} always comes from {@link SecurityIdentity} and never from the
 * request. That single rule is what keeps an id in a path from becoming an IDOR:
 * the owner is a server-side fact, so a caller cannot ask for someone else's row
 * by guessing its id. The scope coming from the identity is binding; the scope
 * being an owner is this project's default (ADR-47).</p>
 */
@Path("/api/demo-items")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Demo Items", description = "CRUD, file attachment and cached summaries for the demo aggregate")
@RolesAllowed("user")
public class DemoItemsResource {

    @Inject
    CreateDemoItemUseCase createDemoItem;

    @Inject
    UpdateDemoItemUseCase updateDemoItem;

    @Inject
    DeleteDemoItemUseCase deleteDemoItem;

    @Inject
    AttachFileToDemoItemUseCase attachFile;

    @Inject
    DownloadDemoItemAttachmentUseCase downloadAttachment;

    @Inject
    ArchiveStaleDemoItemsUseCase archiveStaleDemoItems;

    @Inject
    DemoItemQueryPort query;

    @Inject
    DemoItemSummaries summaries;

    @Inject
    DownloadTokenPort downloadTokens;

    @Inject
    SecurityIdentity identity;

    @ConfigProperty(name = "starter.api.base-url")
    String apiBaseUrl;

    @Operation(summary = "List demo items", description = "Owner-scoped, newest first, paginated. Not cached: page and size are caller-chosen, so cache entries would rarely be read twice.")
    @APIResponse(responseCode = "200", description = "One page of demo items")
    @GET
    public PagedResult<DemoItemResponse> list(
            @Parameter(description = "One-based page index") @QueryParam("page") Integer page,
            @Parameter(description = "Items per page, capped server-side") @QueryParam("pageSize") Integer pageSize) {

        return PagedResult.of(query.pageForOwner(PageRequest.of(page, pageSize), ownerId()), this::toResponse);
    }

    /**
     * Poll endpoint for clients that want "did anything change" cheaply.
     *
     * <p>Two savings stack here. The cached snapshot spares the database, and the
     * {@code ETag} spares the response body: an unchanged list answers 304 with
     * zero bytes. {@code evaluatePreconditions} is what turns the validator into
     * a conditional GET — the platform already implements the comparison, so
     * there is no reason to parse {@code If-None-Match} by hand.</p>
     */
    @Operation(summary = "Cached demo item summaries", description = "Small owner-scoped projection behind a cache, with an ETag for conditional GETs. Returns 304 when nothing changed.")
    @APIResponse(responseCode = "200", description = "Current summaries")
    @APIResponse(responseCode = "304", description = "Client's ETag still matches")
    @GET
    @Path("/summary")
    public Response summaries(@Context Request request) {
        DemoItemSummariesCache.Snapshot snapshot = summaries.snapshot(ownerId());
        EntityTag etag = new EntityTag(snapshot.etag());

        Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
        if (builder == null) {
            builder = Response.ok(snapshot.items());
        }
        return builder.tag(etag).build();
    }

    @Operation(summary = "Get one demo item")
    @APIResponse(responseCode = "200", description = "The demo item")
    @APIResponse(responseCode = "404", description = "No such item for this owner")
    @GET
    @Path("/{id}")
    public DemoItemResponse byId(@PathParam("id") UUID id) {
        return toResponse(query.byIdForOwner(id, ownerId()));
    }

    /**
     * 201 with a {@code Location} header built from {@link UriInfo} rather than a
     * hand-assembled string: the URI has to survive being behind a reverse proxy,
     * and the container already knows the effective request URI.
     */
    @Operation(summary = "Create a demo item", description = "Starts in DRAFT. Fires a DemoItemCreated event after the transaction commits.")
    @APIResponse(responseCode = "201", description = "Created; Location points at the new item")
    @APIResponse(responseCode = "400", description = "Validation failed (application/problem+json)")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid @NotNull DemoItemRequest request, @Context UriInfo uriInfo) {
        DemoItem created = createDemoItem
                .create(new CreateDemoItemCommand(request.name(), request.description(), ownerId())).item();
        summaries.invalidate(ownerId());

        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id().toString()).build())
                .entity(toResponse(created)).build();
    }

    @Operation(summary = "Update a demo item", description = "Requires expectedVersion. Answers 409 if the item changed in the meantime.")
    @APIResponse(responseCode = "200", description = "Updated item, carrying the new version")
    @APIResponse(responseCode = "409", description = "Concurrent modification (application/problem+json)")
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public DemoItemResponse update(@PathParam("id") UUID id, @Valid @NotNull DemoItemRequest request) {
        if (request.expectedVersion() == null) {
            throw new BadRequestException("expectedVersion is required for an update");
        }
        DemoItemStatus status = request.status() == null ? DemoItemStatus.DRAFT : request.status();

        DemoItem updated = updateDemoItem.update(new UpdateDemoItemCommand(id, ownerId(), request.name(),
                request.description(), status, request.expectedVersion())).item();
        summaries.invalidate(ownerId());

        return toResponse(updated);
    }

    @Operation(summary = "Delete a demo item", description = "Removes the item and its stored attachment.")
    @APIResponse(responseCode = "204", description = "Deleted")
    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        deleteDemoItem.delete(id, ownerId());
        summaries.invalidate(ownerId());
        return Response.noContent().build();
    }

    /**
     * The multipart part is converted to a transport-agnostic
     * {@link de.ingoschindler.kernel.upload.UploadedFile} right here, so that
     * {@code FileUpload} — and with it all of JAX-RS — stops at the adapter
     * boundary instead of appearing in port signatures behind it.
     */
    @Operation(summary = "Attach a file", description = "Stores the file in the object store and records it on the item. Request size is capped by quarkus.http.limits.max-body-size.")
    @APIResponse(responseCode = "200", description = "Item with its new attachment and a signed download URL")
    @POST
    @Path("/{id}/attachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public DemoItemResponse attach(@PathParam("id") UUID id, @RestForm("file") FileUpload file) {
        if (file == null) {
            throw new BadRequestException("multipart part 'file' is required");
        }
        DemoItem attached = attachFile.attach(new AttachFileToDemoItemCommand(id, ownerId(), UploadedFiles.from(file)))
                .item();
        summaries.invalidate(ownerId());

        return toResponse(attached);
    }

    /**
     * Authorized by the signed token in the query string, not by the bearer
     * token, so that a plain {@code <img src>} or download link works
     * (ADR-19). Hence {@code @PermitAll} on this one method — and hence the
     * explicit validation as its first statement.
     */
    @Operation(summary = "Download the attachment", description = "Authorized by the short-lived signed token from the item's downloadUrl, not by the bearer token.")
    @APIResponse(responseCode = "200", description = "The file bytes")
    @APIResponse(responseCode = "404", description = "Unknown item, no attachment, or an invalid or expired token")
    @GET
    @Path("/{id}/attachment")
    @PermitAll
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response download(@PathParam("id") UUID id,
            @Parameter(description = "Signed download token") @QueryParam("t") String token) {

        if (token == null || !downloadTokens.validate(token, id)) {
            // 404 rather than 401: a bad token must not confirm that the id exists.
            throw new NotFoundException();
        }
        var result = downloadAttachment.download(id);

        return Response.ok(result.content(), result.contentType()).header("Content-Length", result.size())
                .header("Content-Disposition", "attachment; filename=\"" + result.fileName() + "\"").build();
    }

    /**
     * Manual trigger for the job that otherwise runs on a schedule. Admin-only,
     * and worth having: an operator who can only wait for the next cron tick
     * cannot verify a fix during an incident.
     */
    @Operation(summary = "Run the archive job now", description = "Same use case the scheduler drives. Returns how many items were archived.")
    @APIResponse(responseCode = "200", description = "Number of archived items")
    @POST
    @Path("/archive-stale")
    @RolesAllowed("admin")
    public ArchiveResult archiveStale() {
        int archived = archiveStaleDemoItems.archiveStale();
        // Owner-scoped invalidation cannot help here: the job crosses owners, so the
        // whole cache goes. That is the trade for a job that runs a few times a day.
        summaries.invalidateAll();
        return new ArchiveResult(archived);
    }

    /** Response of the manual archive trigger. */
    public record ArchiveResult(int archived) {
    }

    private DemoItemResponse toResponse(DemoItem item) {
        return DemoItemMapper.toResponse(item, downloadTokens, apiBaseUrl);
    }

    private String ownerId() {
        return identity.getPrincipal().getName();
    }
}
