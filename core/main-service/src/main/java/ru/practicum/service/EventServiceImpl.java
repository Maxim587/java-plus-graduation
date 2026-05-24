package ru.practicum.service;

import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.*;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Category;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final LocationMapper locationMapper;
    private final EventServiceHelper helper;

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto newEventDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Category category = categoryRepository.findById(newEventDto.getCategory())
                .orElseThrow(() -> new NotFoundException("Категория с id = " + newEventDto.getCategory() + " не найдена"));

        LocalDateTime eventDate = LocalDateTime.parse(newEventDto.getEventDate(), DATE_TIME_FORMATTER);
        EventServiceHelper.checkEventDateIsValid(eventDate);
        Event savedEvent = eventRepository.save(EventMapper.mapToEvent(newEventDto, user, category, eventDate));
        EventFullDto dto = EventMapper.mapToFullDto(savedEvent);
        dto.setViews(0L);
        dto.setConfirmedRequests(0L);
        log.info("Создано событие: {}", dto);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateByUser(Long userId, Long eventId, UpdateEventUserRequest request) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
        Optional.ofNullable(request.getCategory()).ifPresent(id -> event.setCategory(helper.getCategory(id)));
        EventServiceHelper.updateEventFieldsFromUserRequest(request, event);
        EventFullDto dto = EventMapper.mapToFullDto(eventRepository.save(event));
        dto = helper.setEventFullDtoFields(dto, event);
        log.info("Событие обновлено Пользователем: {}", dto);
        return dto;
    }

    @Override
    @Transactional
    public EventFullDto updateByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = helper.getEvent(eventId);
        Optional.ofNullable(request.getCategory()).ifPresent(id -> event.setCategory(helper.getCategory(id)));
        Optional.ofNullable(request.getLocation()).ifPresent(loc -> event.setLocation(locationMapper.mapLocationToEventLocation(loc)));
        EventServiceHelper.updateEventFieldsFromAdminRequest(request, event);
        EventFullDto dto = EventMapper.mapToFullDto(eventRepository.save(event));
        helper.setEventFullDtoFields(dto, event);
        log.info("Событие обновлено Администратором: {}", dto);
        return dto;
    }

    @Override
    public EventFullDto getByUser(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
        EventFullDto dto = EventMapper.mapToFullDto(event);
        dto = helper.setEventFullDtoFields(dto, event);
        log.info("Событие получено Пользователем: {}", dto);
        return dto;
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId) {
        log.info("Запрос на поиск события по id: {}", eventId);
        Event event = helper.getEvent(eventId);
        EventServiceHelper.checkEventIsPublished(event);
        EventFullDto dto = EventMapper.mapToFullDto(event);
        dto = helper.setEventFullDtoFields(dto, event);
        return dto;
    }

    @Override
    public List<EventShortDto> getAllByUser(Long userId, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        Set<Event> events = eventRepository.findByInitiatorId(userId, pageable).toSet();
        return helper.getEventShortDtoList(events).stream().toList();
    }

    @Override
    public List<EventFullDto> searchForAdmin(EventSearchRequestAdmin param) {
        EventServiceHelper.checkDates(param.getRangeStart(), param.getRangeEnd());
        PageRequest page = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        Optional<Predicate> searchCriteriaOpt = EventServiceHelper.getAdminSearchCriteria(param);
        List<Event> events = searchCriteriaOpt.map(predicate -> eventRepository.findAll(predicate, page).getContent())
                .orElseGet(() -> eventRepository.findAll(page).getContent());
        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<Long, Long> confirmedRequestsMap = helper.getConfirmedRequests(eventIds);
        Map<Long, Long> viewsMap = helper.getEventsViews(events);
        Map<Long, List<CommentDto>> commentsMap = helper.getCommentsMap(eventIds);

        return events.stream()
                .map(event -> {
                    EventFullDto dto = EventMapper.mapToFullDto(event);
                    dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
                    dto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(dto.getId(), 0L));
                    dto.setComments(commentsMap.getOrDefault(dto.getId(), Collections.emptyList()));
                    return dto;
                })
                .toList();
    }

    @Override
    public List<EventShortDto> searchForUser(EventSearchRequestUser param) {
        EventServiceHelper.checkDates(param.getRangeStart(), param.getRangeEnd());
        PageRequest page = EventServiceHelper.getUserSearchPage(param);
        Predicate searchCriteria = EventServiceHelper.getUserSearchCriteria(param);
        List<Event> events = eventRepository.findAll(searchCriteria, page).getContent();
        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        Map<Long, Long> viewsMap = helper.getEventsViews(events);
        Map<Long, Long> confirmedRequestsMap = helper.getConfirmedRequests(eventIds);
        Map<Long, List<CommentDto>> commentsMap = helper.getCommentsMap(eventIds);

        List<EventShortDto> dtos = new ArrayList<>();
        for (Event event : events) {
            long requestsCount = confirmedRequestsMap.getOrDefault(event.getId(), 0L);
            if (param.getOnlyAvailable() && event.getParticipantLimit() > 0 && requestsCount == event.getParticipantLimit()) {
                continue;
            }
            EventShortDto dto = EventMapper.mapToShortDto(event);
            dto.setConfirmedRequests(requestsCount);
            dto.setViews(viewsMap.getOrDefault(dto.getId(), 0L));
            dto.setComments(commentsMap.getOrDefault(dto.getId(), Collections.emptyList()));
            dtos.add(dto);
        }
        return dtos;
    }


}
