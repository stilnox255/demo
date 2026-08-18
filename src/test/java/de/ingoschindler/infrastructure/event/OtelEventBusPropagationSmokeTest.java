package de.ingoschindler.infrastructure.event;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Smoke test for OpenTelemetry trace-context propagation across the Vert.x event bus. Starts a publisher span,
 * publishes a message, and lets a non-blocking consumer record the trace context of the span active inside the
 * handler. The test passes when Quarkus' OTel-Vert.x integration carries {@code traceparent} through the event
 * bus headers automatically; if it does not, the captured traceId differs from the publisher traceId and the
 * service falls back to a small custom bridge (see plan step 7).
 */
@QuarkusTest
class OtelEventBusPropagationSmokeTest {

    static final String TEST_ADDRESS = "test.otel.eventbus.propagation";

    @Inject
    EventBus eventBus;

    @Inject
    Tracer tracer;

    @Inject
    TraceCapturingConsumer consumer;

    @Test
    void traceparent_propagates_from_publisher_to_consumer_through_vertx_event_bus() {
        consumer.reset();
        Span publisher = tracer.spanBuilder("publisher").startSpan();
        String publisherTraceId;
        try (var _ = publisher.makeCurrent()) {
            publisherTraceId = publisher.getSpanContext().getTraceId();
            eventBus.publish(TEST_ADDRESS, new JsonObject().put("ping", "pong"));
        } finally {
            publisher.end();
        }

        Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> consumer.captured() != null);

        String consumerTraceId = consumer.captured();
        assertNotEquals("00000000000000000000000000000000", consumerTraceId,
                "consumer must run with an active OTel span (non-zero traceId)");
        assertEquals(publisherTraceId, consumerTraceId,
                "Quarkus OTel must propagate traceparent over the Vert.x event-bus headers");
    }

    @ApplicationScoped
    public static class TraceCapturingConsumer {

        private final AtomicReference<String> captured = new AtomicReference<>();

        @ConsumeEvent(TEST_ADDRESS)
        public void onEvent(JsonObject body) {
            captured.set(Span.current().getSpanContext().getTraceId());
        }

        public String captured() {
            return captured.get();
        }

        public void reset() {
            captured.set(null);
        }
    }
}
