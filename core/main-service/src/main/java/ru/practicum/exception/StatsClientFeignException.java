package ru.practicum.exception;

public class StatsClientFeignException extends RuntimeException {
    public StatsClientFeignException(String message) {
        super(message);
    }
}
