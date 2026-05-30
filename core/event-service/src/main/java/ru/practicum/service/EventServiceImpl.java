package ru.practicum.service;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.*;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.Event;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private final EventRepository eventRepository;
    private final EventServiceHelper helper;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(), DATE_TIME_FORMATTER);
        EventServiceHelper.checkEventDateIsValid(eventDate);
        Event savedEvent = eventRepository.save(EventMapper.mapToEvent(newEventDto, userId, eventDate));
        EventFullDto dto = helper.getEventFullDto(savedEvent);
        log.info("Создано событие: {}", dto);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = helper.getEventByIdAndInitiatorId(eventId, userId);
        helper.updateEventFieldsFromUserRequest(request, event);
        Event eventSaved = eventRepository.save(event);
        EventFullDto dto = helper.getEventFullDto(eventSaved);
        log.info("Событие обновлено Пользователем: {}", dto);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = helper.getEvent(eventId);
        helper.updateEventFieldsFromAdminRequest(request, event);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Событие обновлено Администратором: {}", dto);
        return dto;
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        Event event = helper.getEventByIdAndInitiatorId(eventId, userId);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Событие получено Пользователем: {}", dto);
        return dto;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        Event event = helper.getEvent(eventId);
        EventServiceHelper.checkEventIsPublished(event);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Событие получено неавторизованным пользователем: {}", dto);
        return dto;
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Set<Event> events = eventRepository.findByInitiatorId(userId, pageable).toSet();
        return helper.getEventShortDtoList(events, false);
    }

    @Override
    public List<EventFullDto> searchByAdmin(EventSearchRequestAdmin request) {
        EventServiceHelper.checkDates(request.getRangeStart(), request.getRangeEnd());
        PageRequest page = PageRequest.of(request.getFrom() / request.getSize(), request.getSize());
        Optional<Predicate> searchCriteriaOpt = EventServiceHelper.getAdminSearchCriteria(request);
        Set<Event> events = searchCriteriaOpt.map(predicate -> eventRepository.findAll(predicate, page).toSet())
                .orElseGet(() -> eventRepository.findAll(page).toSet());
        return helper.getEventFullDtoList(events);
    }

    @Override
    public List<EventShortDto> searchByUser(EventSearchRequestUser param) {
        EventServiceHelper.checkDates(param.getRangeStart(), param.getRangeEnd());
        PageRequest page = EventServiceHelper.getUserSearchPage(param);
        Predicate searchCriteria = EventServiceHelper.getUserSearchCriteria(param);
        Set<Event> events = eventRepository.findAll(searchCriteria, page).toSet();
        return helper.getEventShortDtoList(events, param.getOnlyAvailable() == null ? false : param.getOnlyAvailable());
    }

    @Override
    public EventInternalDto getEventByIdInternal(Long eventId) {
        Event event = helper.getEvent(eventId);
        return EventMapper.mapToInternalDto(event);
    }

    @Override
    public EventInternalDto getExistingEventInternal(Long categoryId, Long initiatorId) {
        return eventRepository.getFirstByCategoryIdOrInitiatorId(categoryId, initiatorId)
                .map(EventMapper::mapToInternalDto)
                .orElse(null);
    }
}
