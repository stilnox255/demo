package de.ingoschindler.demo.adapter.in.messaging;

import de.ingoschindler.demo.domain.event.DemoItemCreated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.jboss.logging.Logger;

/**
 * Reacts to a newly created item.
 *
 * <p>An inbound adapter like any other: the event bus is a transport, so the
 * handler lives next to the REST resources rather than inside the application
 * layer.</p>
 *
 * <p>{@code TransactionPhase.AFTER_SUCCESS} is the whole point. Observed without
 * it, this runs while the producing transaction is still open — so a rollback
 * afterwards leaves an e-mail sent, a webhook fired or a notification stored for
 * an item that does not exist. Reacting after commit is the boring, correct
 * default; wanting to participate in the transaction is the special case that
 * needs arguing for.</p>
 *
 * <p>Whatever a real handler does here (notify, project, enqueue) must be able to
 * fail without taking the writer down with it: by this point the write is already
 * committed and cannot be undone.</p>
 */
@ApplicationScoped
public class DemoItemCreatedListener {

    private static final Logger LOGGER = Logger.getLogger(DemoItemCreatedListener.class);

    public void onCreated(@Observes(during = TransactionPhase.AFTER_SUCCESS) DemoItemCreated event) {
        LOGGER.infof("demo_item_created id=%s owner=%s occurred_at=%s", event.id(), event.ownerId(),
                event.occurredAt());
    }
}
