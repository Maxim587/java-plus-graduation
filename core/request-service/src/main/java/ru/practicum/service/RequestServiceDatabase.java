package ru.practicum.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.enums.ParticipationRequestStatus;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceDatabase {
    private final ParticipationRequestRepository requestRepository;
    private final ParticipationRequestMapper mapper;

    @Transactional
    public ParticipationRequest saveRParticipationRequestInDatabase(ParticipationRequest request) {
        log.info("Сохранение запроса в БД");
        ParticipationRequest requestDb = requestRepository.save(request);
        log.info("Завершено сохранение запроса в БД");
        return requestDb;
    }

    List<ParticipationRequestDto> findRequestsByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(mapper::mapToParticipationRequestDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult handleChangeRequestStatus(EventRequestStatusUpdateRequest dto, EventInternalDto event) {
        log.info("Проверка запроса и выполнение изменения статусов запросов на участие eventId={}", event.getId());
        ParticipationRequestStatus status = ParticipationRequestStatus.fromString(dto.getStatus());

        if (!(status.equals(ParticipationRequestStatus.CONFIRMED) ||
              status.equals(ParticipationRequestStatus.REJECTED))) {
            throw new ConditionsConflictException("Новый статус для заявок может принимать значения CONFIRMED или REJECTED. Передан " + status);
        }

        if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
            return new EventRequestStatusUpdateResult(Collections.emptyList(), Collections.emptyList());
        }

        int confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED);
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

        log.info("Завершена проверка запроса и выполнение изменения статусов запросов на участие eventId={}", event.getId());
        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }

    private void checkStatus(ParticipationRequest request) {
        if (request.getStatus() != ParticipationRequestStatus.PENDING) {
            throw new ConditionsConflictException("Заявки из списка должны иметь статус PENDING");
        }
    }
}
