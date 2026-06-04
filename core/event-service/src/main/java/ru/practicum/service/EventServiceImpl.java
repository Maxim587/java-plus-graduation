package ru.practicum.service;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
public class EventServiceImpl implements EventService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private final EventRepository eventRepository;
    private final EventServiceHelper helper;

    @Override
    public EventFullDto create(NewEventDto newEventDto) {
        log.info("Обработка запроса на создание события");
        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(), DATE_TIME_FORMATTER);
        EventServiceHelper.checkEventDateIsValid(eventDate);
        Event event = EventMapper.mapToEvent(newEventDto, eventDate);
        Event savedEvent = helper.saveEventInDatabase(event);
        EventFullDto dto = helper.getEventFullDto(savedEvent);
        log.info("Завершена обработка запроса на создание события. Создано событие id={}", dto.getId());
        return dto;
    }

    @Override
    public EventFullDto updateByUser(UpdateEventUserRequest request) {
        log.info("Обработка запроса на обновление события Пользователем");
        Event event = helper.getEventByIdAndInitiatorId(request.getEventId(), request.getUserId());
        Event eventSaved = helper.updateEventFieldsFromUserRequest(request, event);
        EventFullDto dto = helper.getEventFullDto(eventSaved);
        log.info("Завершена обработка запроса на обновление события Пользователем");
        return dto;
    }

    @Override
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обработка запроса на обновление события Администратором");
        Event event = helper.updateEventFieldsFromAdminRequest(request, eventId);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Завершена обработка запроса на обновление события Администратором");
        return dto;
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        log.info("Обработка запроса на получение события Пользователем");
        Event event = helper.getEventByIdAndInitiatorId(eventId, userId);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Завершена обработка запроса на получение события Пользователем");
        return dto;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("Обработка запроса на получение события неавторизованным Пользователем");
        Event event = helper.getEvent(eventId);
        EventServiceHelper.checkEventIsPublished(event);
        EventFullDto dto = helper.getEventFullDto(event);
        log.info("Завершена обработка запроса на получение события неавторизованным Пользователем");
        return dto;
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        log.info("Обработка запроса на получение списка событий инициатора");
        Pageable pageable = PageRequest.of(from / size, size);
        Set<Event> events = eventRepository.findByInitiatorId(userId, pageable).toSet();
        List<EventShortDto> dtoList = helper.getEventShortDtoList(events, false);
        log.info("Завершена обработка запроса на получение списка событий инициатора");
        return dtoList;
    }

    @Override
    public List<EventFullDto> searchByAdmin(EventSearchRequestAdmin request) {
        log.info("Обработка запроса на поиск событий Администратором");
        EventServiceHelper.checkDates(request.getRangeStart(), request.getRangeEnd());
        PageRequest page = PageRequest.of(request.getFrom() / request.getSize(), request.getSize());
        Optional<Predicate> searchCriteriaOpt = EventServiceHelper.getAdminSearchCriteria(request);
        Set<Event> events = searchCriteriaOpt.map(predicate -> eventRepository.findAll(predicate, page).toSet())
                .orElseGet(() -> eventRepository.findAll(page).toSet());
        return helper.getEventFullDtoList(events);
    }

    @Override
    public List<EventShortDto> searchByUser(EventSearchRequestUser param) {
        log.info("Обработка запроса на поиск событий Пользователем");
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
