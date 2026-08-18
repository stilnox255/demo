package de.ingoschindler.infrastructure.logging;

import org.jboss.logmanager.ExtLogRecord;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Test-only helper that attaches a {@link Handler} to the root logger and captures records together with their
 * MDC snapshot. Used by the logging tests to assert structured fields without parsing JSON output.
 */
public final class LogRecording implements AutoCloseable {

    public record Captured(String message, Level level, Map<String, String> mdc, String loggerName) {
    }

    private final List<Captured> records = new CopyOnWriteArrayList<>();
    private final Logger root;
    private final Handler handler;

    private LogRecording(Logger root, Handler handler) {
        this.root = root;
        this.handler = handler;
    }

    public static LogRecording start() {
        Logger root = Logger.getLogger("");
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() throws SecurityException {
            }
        };
        LogRecording recording = new LogRecording(root, new RecordingHandler(handler));
        root.addHandler(recording.handler);
        recording.handler.setLevel(Level.ALL);
        ((RecordingHandler) recording.handler).bind(recording.records);
        return recording;
    }

    public List<Captured> records() {
        return List.copyOf(records);
    }

    @Override
    public void close() {
        root.removeHandler(handler);
    }

    private static final class RecordingHandler extends Handler {
        private final Handler delegate;
        private List<Captured> sink;

        RecordingHandler(Handler delegate) {
            this.delegate = delegate;
        }

        void bind(List<Captured> target) {
            this.sink = target;
        }

        @Override
        public void publish(LogRecord record) {
            Map<String, String> mdc = Map.of();
            if (record instanceof ExtLogRecord ext && ext.getMdcCopy() != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> snapshot = (Map<String, String>) (Map<?, ?>) ext.getMdcCopy();
                mdc = Map.copyOf(snapshot);
            }
            sink.add(new Captured(record.getMessage(), record.getLevel(), mdc, record.getLoggerName()));
            delegate.publish(record);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() throws SecurityException {
            delegate.close();
        }
    }
}
