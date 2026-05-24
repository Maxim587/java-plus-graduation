package ru.practicum.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ViewStatsDto;
import ru.practicum.dto.*;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.StatsClientFeignException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.StatsClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.mapper.EventMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.model.EventStateAdmin.PUBLISH_EVENT;
import static ru.practicum.model.EventStateAdmin.REJECT_EVENT;
import static ru.practicum.service.EventServiceImpl.DATE_TIME_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceHelper {
    private static final long MIN_HOURS_BETWEEN_EVENT_DATE_AND_PUBLISH_DATE = 1L;
    private static final long MIN_HOURS_FROM_NOW_TO_EVENT_DATE = 2L;
    private static final String EVENTS_ENDPOINT = "/events/";
    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final StatsClient statsClient;

    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    public Map<Long, List<CommentDto>> getCommentsMap(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<CommentDto>> commentsMap = new HashMap<>();
        commentRepository.findAllByEventIdIn(eventIds).forEach(comment -> {
            commentsMap.computeIfAbsent(comment.getEvent().getId(), eventId -> new ArrayList<>())
                    .add(commentMapper.mapToCommentDto(comment));
        });
        return commentsMap;
    }

    public Long getEventViews(Event event) {
        log.info("Получение просмотров у события, id={}", event.getId());
        if (event.getPublishedOn() == null) {
            log.info("Событие не опубликовано, возвращается 0 просмотров");
            return 0L;
        }

        List<String> uris = List.of(EVENTS_ENDPOINT + event.getId());
        LocalDateTime start = event.getPublishedOn().truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        try {
            log.info("Отправка запроса на получение статистики с параметрами: start={}, end={}, uris={}", start, end, uris);
            List<ViewStatsDto> viewStatsDtos = statsClient.getStats(start, end, uris, true);
            log.info("Ответ от сервера статистики {}:", viewStatsDtos);
            Long viewsCount = viewStatsDtos
                    .stream()
                    .findFirst()
                    .map(stats -> stats.getHits() != null ? stats.getHits() : 0L)
                    .orElse(0L);
            log.info("Количество просмотров {}:", viewsCount);
            return viewsCount;
        } catch (FeignException e) {
            log.error("Ошибка feign-клиента сервиса статистики: {}", e.getMessage());
            throw new StatsClientFeignException(e.getMessage());
        }
    }

    public Map<Long, Long> getEventsViews(List<Event> events) {
        List<String> uris = new ArrayList<>();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start;
        for (Event event : events) {
            uris.add(EVENTS_ENDPOINT + event.getId());
            if (event.getPublishedOn() != null && start.isAfter(event.getPublishedOn())) {
                start = event.getPublishedOn();
            }
        }
        if (start.isEqual(end)) {
            return Collections.emptyMap();
        }

        List<ViewStatsDto> viewDtos;
        try {
            log.info("Отправка запроса на получение статистики с параметрами: start={}, end={}, uris={}", start, end, uris);
            viewDtos = statsClient.getStats(start, end, uris, true);
            log.info("Ответ от сервера статистики {}:", viewDtos);
        } catch (FeignException e) {
            log.error("Ошибка feign-клиента сервиса статистики: {}", e.getMessage());
            throw new StatsClientFeignException(e.getMessage());
        }

        Map<Long, Long> viewsMap = new HashMap<>();
        for (ViewStatsDto view : viewDtos) {
            String[] parts = view.getUri().split("/");
            if (parts.length == 3) {
                Long eventId = Long.parseLong(parts[parts.length - 1]);
                viewsMap.put(eventId, view.getHits());
            }
        }
        return viewsMap;
    }

    public EventFullDto setEventFullDtoFields(EventFullDto dto, Event event) {
        log.info("Заполнение полей Views, ConfirmedRequests, Comments у события");
        dto.setViews(getEventViews(event));
        dto.setConfirmedRequests((long) requestRepository.countByEventIdAndStatus(event.getId(), ParticipationRequestStatus.CONFIRMED));
        dto.setComments(getCommentsDtoList(event.getId()));
        return dto;
    }

    public List<CommentDto> getCommentsDtoList(Long eventId) {
        return commentRepository.findByEventId(eventId).stream()
                .map(commentMapper::mapToCommentDto)
                .toList();
    }

    public Map<Long, Long> getConfirmedRequests(Set<Long> eventIds) {
        return requestRepository.getConfirmedRequestsCount(eventIds, ParticipationRequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(object -> (Long) object[0], object -> (Long) object[1]));
    }

    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Категория с id " + id + " не найдена"));
    }

    public Set<EventShortDto> getEventShortDtoList(Set<Event> events) {
        Set<Long> eventsIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        List<Event> eventList = events.stream().toList();
        Map<Long, Long> viewsMap = getEventsViews(eventList);
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequests(eventsIds);
        Map<Long, List<CommentDto>> commentsMap = getCommentsMap(eventsIds);

        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = EventMapper.mapToShortDto(event);
                    eventShortDto.setViews(viewsMap.getOrDefault(eventShortDto.getId(), 0L));
                    eventShortDto.setConfirmedRequests(confirmedRequestsMap.getOrDefault(eventShortDto.getId(), 0L));
                    eventShortDto.setComments(commentsMap.getOrDefault(eventShortDto.getId(), Collections.emptyList()));
                    return eventShortDto;
                })
                .collect(Collectors.toSet());
    }

    public static void updateEventFieldsFromUserRequest(UpdateEventUserRequest request, Event event) {
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConditionsConflictException("Нельзя редактировать опубликованное событие");
        }

        if (request.getEventDate() != null) {
            LocalDateTime eventDate = LocalDateTime.parse(request.getEventDate(), DATE_TIME_FORMATTER);
            checkEventDateIsValid(eventDate);
            event.setEventDate(eventDate);
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getLocation() != null && request.getLocation().getLat() != null
            && request.getLocation().getLon() != null) {
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getStateAction() != null) {
            if (EventStateUser.fromString(request.getStateAction()) == EventStateUser.CANCEL_REVIEW) {
                event.setState(EventState.CANCELED);
            } else if (EventStateUser.fromString(request.getStateAction()) == EventStateUser.SEND_TO_REVIEW) {
                event.setState(EventState.PENDING);
            }
        }
    }

    public static void updateEventFieldsFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (event.getPublishedOn() != null && event.getPublishedOn().isAfter(event.getEventDate().plusHours(MIN_HOURS_BETWEEN_EVENT_DATE_AND_PUBLISH_DATE))) {
            throw new ValidationException("Дата начала изменяемого события должна быть не ранее чем за час от даты публикации");
        }

        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }

        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }

        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getEventDate() != null) {
            LocalDateTime newEventDate = LocalDateTime.parse(request.getEventDate(), DATE_TIME_FORMATTER);
            if (!newEventDate.isAfter(LocalDateTime.now())) {
                throw new ValidationException("Дата события должна быть больше текущей даты");
            }
            event.setEventDate(LocalDateTime.parse(request.getEventDate(), DATE_TIME_FORMATTER));
        }

        if (request.getStateAction() != null) {
            EventStateAdmin stateAdmin = EventStateAdmin.fromString(request.getStateAction());
            if (stateAdmin.equals(PUBLISH_EVENT)) {
                if (event.getState() != EventState.PENDING) {
                    throw new ConditionsConflictException("Событие можно публиковать только если оно в состоянии ожидания публикации");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
            } else if (stateAdmin.equals(REJECT_EVENT)) {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConditionsConflictException("Событие можно отклонить только если оно еще не опубликовано");
                }
                event.setState(EventState.CANCELED);
            }
        }
    }

    public static Predicate getUserSearchCriteria(EventSearchRequestUser req) {
        QEvent event = QEvent.event;
        BooleanBuilder booleanBuilder = new BooleanBuilder();
        booleanBuilder.and(event.state.eq(EventState.PUBLISHED));

        if (req.getText() != null && !req.getText().isBlank()) {
            booleanBuilder.and(event.annotation.containsIgnoreCase(req.getText())
                    .or(event.description.containsIgnoreCase(req.getText())));
        }
        if (req.getCategories() != null && !req.getCategories().isEmpty()) {
            booleanBuilder.and(event.category.id.in(req.getCategories()));
        }
        if (req.getRangeStart() == null && req.getRangeEnd() == null) {
            booleanBuilder.and(event.eventDate.goe(LocalDateTime.now()));
        } else {
            if (req.getRangeStart() != null) {
                booleanBuilder.and(event.eventDate.goe(req.getRangeStart()));
            }
            if (req.getRangeEnd() != null) {
                booleanBuilder.and(event.eventDate.loe(req.getRangeEnd()));
            }
        }
        if (req.getPaid() != null) {
            booleanBuilder.and(event.paid.eq(req.getPaid()));
        }

        return booleanBuilder.getValue();
    }

    public static Optional<Predicate> getAdminSearchCriteria(EventSearchRequestAdmin req) {
        QEvent event = QEvent.event;
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        if (req.getUsers() != null && !req.getUsers().isEmpty()) {
            booleanBuilder.and(event.initiator.id.in(req.getUsers()));
        }
        if (req.getStates() != null && !req.getStates().isEmpty()) {
            booleanBuilder.and(event.state.in(req.getStates()));
        }
        if (req.getCategories() != null && !req.getCategories().isEmpty()) {
            booleanBuilder.and(event.category.id.in(req.getCategories()));
        }
        if (req.getRangeStart() != null) {
            booleanBuilder.and(event.eventDate.goe(req.getRangeStart()));
        }
        if (req.getRangeEnd() != null) {
            booleanBuilder.and(event.eventDate.loe(req.getRangeEnd()));
        }
        return Optional.ofNullable(booleanBuilder.getValue());
    }

    public static PageRequest getUserSearchPage(EventSearchRequestUser param) {
        if (getUserSearchSort(param).isEmpty()) {
            return PageRequest.of(param.getFrom() / param.getSize(), param.getSize());
        } else {
            return PageRequest.of(param.getFrom() / param.getSize(), param.getSize(), getUserSearchSort(param).get());
        }
    }

    public static void checkDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания.");
        }
    }

    public static void checkEventDateIsValid(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(MIN_HOURS_FROM_NOW_TO_EVENT_DATE))) {
            throw new ValidationException("Дата события должна быть не раньше чем через 2 часа от текущего момента");
        }
    }

    public static void checkEventIsPublished(Event event) {
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Событие не опубликовано");
        }
    }

    private static Optional<Sort> getUserSearchSort(EventSearchRequestUser req) {
        if (req.getSort() == null) {
            return Optional.empty();
        }
        EventUserSort userSort = EventUserSort.fromString(req.getSort());
        String sortColumn = switch (userSort) {
            case EVENT_DATE -> "eventDate";
            case VIEWS -> "views";
        };
        return Optional.of(Sort.by(Sort.Direction.DESC, sortColumn));
    }
}
