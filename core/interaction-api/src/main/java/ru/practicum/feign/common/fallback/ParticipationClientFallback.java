package ru.practicum.feign.common.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.participation.ParticipationClient;

import java.util.List;

@Slf4j
@Component
public class ParticipationClientFallback implements ParticipationClient {

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: request service is unavailable");
    }
}
