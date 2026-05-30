package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.ParticipationRequestStatus;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.internal.EventClientInternal;
import ru.practicum.feign.internal.UserClientInternal;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper mapper;
    private final UserClientInternal userClientInternal;
    private final EventClientInternal eventClientInternal;


    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        EventInternalDto event = eventClientInternal.getEventByIdInternal(eventId);
        ParticipationRequest request = createParticipationRequest(userId, event);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
        }
        return mapper.mapToParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(mapper::mapToParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.getByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Запрос с id " + requestId
                                                         + " для пользователя с id " + userId + " не найден"));

        if (request.getStatus() == ParticipationRequestStatus.REJECTED ||
            request.getStatus() == ParticipationRequestStatus.CANCELED) {
            throw new ConditionsConflictException("Заявка находится в статусе " + request.getStatus() + ". Отмена заявки невозможна");
        }

        request.setStatus(ParticipationRequestStatus.CANCELED);
        return mapper.mapToParticipationRequestDto(requestRepository.save(request));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        EventInternalDto event = eventClientInternal.getEventByIdInternal(eventId);
        checkRequesterIsEventInitiator(userId, event);
        ParticipationRequestStatus status = ParticipationRequestStatus.fromString(dto.getStatus());

        if (!(status.equals(ParticipationRequestStatus.CONFIRMED) ||
              status.equals(ParticipationRequestStatus.REJECTED))) {
            throw new ConditionsConflictException("Новый статус для заявок может принимать значения CONFIRMED или REJECTED. Передан " + status);
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            return new EventRequestStatusUpdateResult(Collections.emptyList(), Collections.emptyList());
        }

        int confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
        int possibleToConfirmCount = event.getParticipantLimit() - confirmedRequests;

        if (status == ParticipationRequestStatus.CONFIRMED && possibleToConfirmCount == 0) {
            throw new ConditionsConflictException("Достигнут лимит на участие у события");
        }

        List<ParticipationRequest> requests = requestRepository.findAllByIdIn(dto.getRequestIds());
        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();
        List<Long> confirmedIds = new ArrayList<>();
        List<Long> rejectedIds = new ArrayList<>();

        if (status == ParticipationRequestStatus.CONFIRMED) {
            for (int i = 0; i < requests.size(); i++) {
                ParticipationRequest request = requests.get(i);
                checkStatus(request);
                if (i + 1 <= possibleToConfirmCount) {
                    ParticipationRequestDto confirmedDto = mapper.mapToParticipationRequestDto(request);
                    confirmedDto.setStatus(ParticipationRequestStatus.CONFIRMED.name());
                    confirmed.add(confirmedDto);
                    confirmedIds.add(request.getId());
                } else {
                    ParticipationRequestDto rejectedDto = mapper.mapToParticipationRequestDto(request);
                    rejectedDto.setStatus(ParticipationRequestStatus.REJECTED.name());
                    rejected.add(rejectedDto);
                    rejectedIds.add(request.getId());
                }
            }
            requestRepository.updateStatus(ParticipationRequestStatus.CONFIRMED, confirmedIds);
            if (!rejectedIds.isEmpty()) {
                requestRepository.updateStatus(ParticipationRequestStatus.REJECTED, rejectedIds);
            }
        } else {
            for (ParticipationRequest request : requests) {
                checkStatus(request);
                ParticipationRequestDto rejectedDto = mapper.mapToParticipationRequestDto(request);
                rejectedDto.setStatus(ParticipationRequestStatus.REJECTED.name());
                rejected.add(rejectedDto);
                rejectedIds.add(request.getId());
            }
            requestRepository.updateStatus(ParticipationRequestStatus.REJECTED, rejectedIds);
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        EventInternalDto event = eventClientInternal.getEventByIdInternal(eventId);
        checkRequesterIsEventInitiator(userId, event);
        return requestRepository.findAllByEventId(eventId).stream()
                .map(mapper::mapToParticipationRequestDto)
                .toList();
    }

    @Override
    public long getConfirmedRequestsCount(Long eventId) {
        return requestRepository.countByEventIdAndStatus(eventId, ParticipationRequestStatus.CONFIRMED);
    }

    @Override
    public Map<Long, Long> getEventIdToConfirmedRequestsCount(Set<Long> eventIds) {
        return requestRepository.getEventIdToConfirmedRequestsCount(eventIds, ParticipationRequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(object -> (Long) object[0], object -> (Long) object[1]));
    }

    @Override
    public boolean existsByRequesterIdInternal(Long requesterId) {
        return requestRepository.existsByRequesterId(requesterId);
    }

    private void checkStatus(ParticipationRequest request) {
        if (request.getStatus() != ParticipationRequestStatus.PENDING) {
            throw new ConditionsConflictException("Заявки из списка должны иметь статус PENDING");
        }
    }

    private void checkRequesterIsEventInitiator(Long userId, EventInternalDto event) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConditionsConflictException("Пользователь с id " + userId + " не является инициатором события " + event.getId());
        }
    }

    private ParticipationRequest createParticipationRequest(Long userId, EventInternalDto event) {
        UserShortDto userShortDto = userClientInternal.getUserShortDtoById(userId);
        validateRequest(event, userShortDto.getId());
        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userShortDto.getId());
        request.setEventId(event.getId());
        return request;
    }

    private void validateRequest(EventInternalDto event, Long userId) {
        if (event.getInitiatorId().equals(userId)) {
            throw new ConditionsConflictException("Пользователь с id " + userId +
                                                  " не может создавать заявку на участие в событии с id " + event.getId() +
                                                  " т.к. является его инициатором");
        }

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConditionsConflictException("Нельзя участвовать в неопубликованном событии");
        }

        boolean isDuplicateRequestForSameEvent = requestRepository.existsByEventIdAndRequesterId(event.getId(), userId);
        if (isDuplicateRequestForSameEvent) {
            throw new ConditionsConflictException("Пользователь с id " + userId + " уже подавал заявку на участие в событии id " + event.getId());
        }

        int confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);

        if (event.getParticipantLimit() > 0 && event.getParticipantLimit() == confirmedRequests) {
            throw new ConditionsConflictException("Достигнут лимит на участие у события");
        }
    }
}
