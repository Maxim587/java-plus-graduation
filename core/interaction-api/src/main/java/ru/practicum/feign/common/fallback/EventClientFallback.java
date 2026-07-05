package ru.practicum.feign.common.fallback;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.*;
import ru.practicum.enums.EventState;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.event.admin.EventClientAdmin;
import ru.practicum.feign.common.event.authorized.EventClientAuthorized;
import ru.practicum.feign.common.nonauthorized.EventClientNonauthorized;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class EventClientFallback implements EventClientAdmin, EventClientAuthorized, EventClientNonauthorized {

    @Override
    public List<EventFullDto> searchForAdmin(List<Long> users, List<EventState> states, List<Long> categories, LocalDateTime rangeStart, LocalDateTime rangeEnd, Integer from, Integer size) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public EventFullDto getEvent(Long id, Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<EventShortDto> searchForUser(String text, List<Long> categories, Boolean paid, LocalDateTime rangeStart, LocalDateTime rangeEnd, Boolean onlyAvailable, String sort, Integer from, Integer size, HttpServletRequest request) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<EventShortDto> getRecommendations(Integer maxResults, Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public void likeEvent(Long eventId, Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: event service is unavailable");
    }
}
