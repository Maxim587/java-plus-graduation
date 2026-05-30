package ru.practicum.service;

import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ParticipationRequestService {
    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto);

    List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId);

    long getConfirmedRequestsCount(Long eventId);

    Map<Long, Long> getEventIdToConfirmedRequestsCount(Set<Long> eventIds);

    boolean existsByRequesterIdInternal(Long requesterId);
}
