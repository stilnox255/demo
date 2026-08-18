package de.ingoschindler.demo.adapter.in.rest.dto;

import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.kernel.download.DownloadTokenPort;

/**
 * Domain to wire format. The only place that knows both shapes.
 *
 * <p>Takes the token port as an argument instead of the domain carrying it: a
 * domain method that signs URLs would pull an infrastructure concern into a type
 * that is meant to be testable with plain values.</p>
 */
public final class DemoItemMapper {

    private DemoItemMapper() {
    }

    public static DemoItemResponse toResponse(DemoItem item, DownloadTokenPort tokens, String apiBaseUrl) {
        DemoItemResponse.Attachment attachment = item.findAttachment()
                .map(a -> new DemoItemResponse.Attachment(a.fileName(), a.contentType(), a.size(),
                        tokens.signedUrl(apiBaseUrl, "/api/demo-items/" + item.id() + "/attachment", item.id())))
                .orElse(null);

        return new DemoItemResponse(item.id(), item.name(), item.description(), item.status(), item.createdAt(),
                item.version(), attachment);
    }

    public static DemoItemSummary toSummary(DemoItem item) {
        return new DemoItemSummary(item.id(), item.name(), item.status(), item.findAttachment().isPresent());
    }
}
