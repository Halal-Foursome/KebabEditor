package org.halalfoursome.kebabeditor.logging;

import java.time.LocalDateTime;
import java.util.Objects;

public record LogEntry(LocalDateTime timestamp, LogLevel level, String message, Throwable cause) {
    
    public LogEntry {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(message, "message");
    }
}
