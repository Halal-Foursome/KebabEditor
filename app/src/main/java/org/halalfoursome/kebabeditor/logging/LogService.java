package org.halalfoursome.kebabeditor.logging;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thread-safe in-memory application log. Listeners may run on any calling thread. */
public final class LogService {
    private static final int DEFAULT_CAPACITY = 1000;

    private final int capacity;
    private final List<LogEntry> entries = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    public LogService() {
        this(DEFAULT_CAPACITY);
    }

    public LogService(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.capacity = capacity;
    }

    public void info(String message) {
        log(LogLevel.INFO, message, null);
    }

    public void warn(String message) {
        log(LogLevel.WARN, message, null);
    }

    public void error(String message, Throwable cause) {
        log(LogLevel.ERROR, message, cause);
    }

    public void log(LogLevel level, String message, Throwable cause) {
        LogEntry entry = new LogEntry(LocalDateTime.now(), level, message, cause);
        List<Runnable> subscribers;
        synchronized (this) {
            if (entries.size() == capacity) {
                entries.remove(0);
            }
            entries.add(entry);
            subscribers = List.copyOf(listeners);
        }
        notifyListeners(subscribers);
    }

    public void clear() {
        List<Runnable> subscribers;
        synchronized (this) {
            entries.clear();
            subscribers = List.copyOf(listeners);
        }
        notifyListeners(subscribers);
    }

    public synchronized List<LogEntry> entries() {
        return List.copyOf(entries);
    }

    public synchronized void addListener(Runnable listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners(List<Runnable> subscribers) {
        for (Runnable listener : subscribers) {
            listener.run();
        }
    }
}
