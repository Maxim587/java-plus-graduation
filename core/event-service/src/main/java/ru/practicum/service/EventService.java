package ru.practicum.service;

import ru.practicum.dto.event.*;

import java.util.List;

public interface EventService {

    EventFullDto create(Long userId, NewEventDto newEventDto);

    EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request);

    EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request);

    EventFullDto getByUser(Long userId, Long eventId);

    EventFullDto getPublicEvent(Long eventId);

    List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size);

    List<EventFullDto> searchByAdmin(EventSearchRequestAdmin param);

    List<EventShortDto> searchByUser(EventSearchRequestUser param);

    EventInternalDto getEventByIdInternal(Long eventId);

    EventInternalDto getExistingEventInternal(Long categoryId, Long initiatorId);
}
