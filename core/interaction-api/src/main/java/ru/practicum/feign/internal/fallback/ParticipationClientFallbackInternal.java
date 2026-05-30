package ru.practicum.feign.internal.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.internal.ParticipationClientInternal;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class ParticipationClientFallbackInternal implements ParticipationClientInternal {

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public Map<Long, Long> getEventIdToConfirmedRequestsCount(@RequestBody Set<Long> eventIds) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public boolean existsByRequesterIdInternal(@RequestParam Long requesterId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: participation service is unavailable");
    }
}
