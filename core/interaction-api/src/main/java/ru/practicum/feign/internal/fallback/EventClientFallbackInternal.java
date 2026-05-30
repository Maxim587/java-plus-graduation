package ru.practicum.feign.internal.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.internal.EventClientInternal;

@Slf4j
@Component
public class EventClientFallbackInternal implements EventClientInternal {

    @Override
    public EventInternalDto getEventByIdInternal(Long eventId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventInternalDto getExistingEventInternal(Long categoryId, Long initiatorId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }


    void logError() {
        log.error("Fallback response: event service is unavailable");
    }
}
