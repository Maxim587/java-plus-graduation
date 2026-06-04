package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipationRequestServiceImpl implements ParticipationRequestService {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper mapper;
    private final UserClientInternal userClientInternal;
    private final EventClientInternal eventClientInternal;
    private final RequestServiceDatabase requestServiceDatabase;

    @Override
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        log.info("Начало создания запроса на участие в событии eventId={}", eventId);
        EventInternalDto event = getEventById(eventId);
        ParticipationRequest request = createParticipationRequest(userId, event);

        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            request.setStatus(ParticipationRequestStatus.CONFIRMED);
        }
        request = requestServiceDatabase.saveRParticipationRequestInDatabase(request);
        ParticipationRequestDto dto = mapper.mapToParticipationRequestDto(request);
        log.info("Завершено создания запроса на участие в событии eventId={}", eventId);
        return dto;
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Обработка запроса на поиск запросов на участие пользователя userId={}", userId);
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(mapper::mapToParticipationRequestDto)
                .toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Обработка запроса на отмену запроса на участие userId={}, requestId={}", userId, requestId);
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
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId, EventRequestStatusUpdateRequest dto) {
        log.info("Обработка запроса на изменение статуса запросов на участие");
        EventInternalDto event = getEventById(eventId);
        checkRequesterIsEventInitiator(userId, event);
        return requestServiceDatabase.handleChangeRequestStatus(dto, event);
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        EventInternalDto event = getEventById(eventId);
        checkRequesterIsEventInitiator(userId, event);
        return requestServiceDatabase.findRequestsByEventId(eventId);
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

    private void checkRequesterIsEventInitiator(Long userId, EventInternalDto event) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConditionsConflictException("Пользователь с id " + userId + " не является инициатором события " + event.getId());
        }
    }

    private ParticipationRequest createParticipationRequest(Long userId, EventInternalDto event) {
        log.info("Создание экземпляра запроса на участие в событии eventId={}", event.getId());
        log.info("Получение пользователя через клиент по id={}", userId);
        UserShortDto userShortDto = userClientInternal.getUserShortDtoById(userId);
        log.info("Завершено получение пользователя через клиент по id={}", userId);
        validateRequest(event, userShortDto.getId());
        ParticipationRequest request = new ParticipationRequest();
        request.setRequesterId(userShortDto.getId());
        request.setEventId(event.getId());
        log.info("Завершено создание экземпляра запроса на участие в событии eventId={}", event.getId());
        return request;
    }

    private void validateRequest(EventInternalDto event, Long userId) {
        log.info("Валидация запроса на участие в событии с eventId={}", event.getId());
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
        log.info("Завершена валидация запроса на участие в событии с eventId={}", event.getId());
    }

    private EventInternalDto getEventById(Long eventId) {
        log.info("Получение события через клиент по id={}", eventId);
        EventInternalDto event = eventClientInternal.getEventByIdInternal(eventId);
        log.info("Завершено получение события через клиент по id={}", eventId);
        return event;
    }
}
