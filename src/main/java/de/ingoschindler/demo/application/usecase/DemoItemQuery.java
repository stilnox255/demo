package de.ingoschindler.demo.application.usecase;

import de.ingoschindler.demo.application.port.in.DemoItemQueryPort;
import de.ingoschindler.demo.application.port.out.DemoItemRepository;
import de.ingoschindler.demo.domain.DemoItem;
import de.ingoschindler.demo.domain.DemoItemNotFoundException;
import de.ingoschindler.kernel.pagination.Page;
import de.ingoschindler.kernel.pagination.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Read side of the BC. No {@code @Transactional}: these are plain reads, and
 * wrapping them adds a transaction boundary nobody needs.
 */
@ApplicationScoped
public class DemoItemQuery implements DemoItemQueryPort {

    @Inject
    DemoItemRepository repository;

    @Override
    public DemoItem byIdForOwner(UUID id, String ownerId) {
        return repository.findByIdForOwner(id, ownerId).orElseThrow(() -> new DemoItemNotFoundException(id));
    }

    @Override
    public Page<DemoItem> pageForOwner(PageRequest request, String ownerId) {
        return repository.findPageForOwner(request, ownerId);
    }

    @Override
    public List<DemoItem> allForOwner(String ownerId) {
        return repository.findAllForOwner(ownerId);
    }
}
