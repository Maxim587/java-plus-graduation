package ru.practicum.exception;

public class FeignClientUnavailableException extends RuntimeException {
    public FeignClientUnavailableException(String message) {
        super(message);
    }
}
