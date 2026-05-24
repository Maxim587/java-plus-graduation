package ru.practicum.fallback;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class CustomFeignError {
    private final String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    private final String message;
    private final String reason;
    private final int status;

    public CustomFeignError(String message, String reason, HttpStatus status) {
        this.message = message;
        this.reason = reason;
        this.status = status.value();
    }
}
