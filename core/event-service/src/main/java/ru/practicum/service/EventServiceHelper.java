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
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.EventState;
import ru.practicum.enums.EventStateAdmin;
import ru.practicum.enums.EventStateUser;
import ru.practicum.enums.EventUserSort;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.StatsClient;
import ru.practicum.feign.internal.CategoryClientInternal;
import ru.practicum.feign.internal.CommentClientInternal;
import ru.practicum.feign.internal.ParticipationClientInternal;
import ru.practicum.feign.internal.UserClientInternal;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.Event;
import ru.practicum.model.QEvent;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static ru.practicum.enums.EventStateAdmin.PUBLISH_EVENT;
import static ru.practicum.enums.EventStateAdmin.REJECT_EVENT;
import static ru.practicum.service.EventServiceImpl.DATE_TIME_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceHelper {
    private static final long MIN_HOURS_BETWEEN_EVENT_DATE_AND_PUBLISH_DATE = 1L;
    private static final long MIN_HOURS_FROM_NOW_TO_EVENT_DATE = 2L;
    private static final String EVENTS_ENDPOINT = "/events/";
    private final EventRepository eventRepository;
    private final LocationMapper locationMapper;
    private final StatsClient statsClient;
    private final UserClientInternal userClientInternal;
    private final CommentClientInternal commentClientInternal;
    private final ParticipationClientInternal participationClientInternal;
    private final CategoryClientInternal categoryClientInternal;


    @Transactional
    public Event saveEventInDatabase(final Event event) {
        log.info("Сохранение события в БД, eventId");
        return eventRepository.save(event);
    }

    public Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    public Event getEventByIdAndInitiatorId(Long eventId, Long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id " + eventId + " не найдено"));
    }

    public Long getEventViews(Event event) {
        log.info("Получение просмотров у события, id={}", event.getId());

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
            throw new FeignClientUnavailableException(e.getMessage());
        }
    }

    public Map<Long, Long> getEventIdToViewsCountMap(Set<Event> events) {
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
            throw new FeignClientUnavailableException(e.getMessage());
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

    public EventFullDto getEventFullDto(Event event) {
        log.info("Начало создания EventFullDto для события, eventId={}", event.getId());
        Long eventId = event.getId();
        Long views = 0L;
        Long confirmedRequests = 0L;
        List<CommentDto> commentDtoList = new ArrayList<>();

        if (event.getPublishedOn() != null) {
            views = getEventViews(event);
            log.info("Получение подтвержденных запросов через клиент для события, eventId={}", eventId);
            confirmedRequests = participationClientInternal.getConfirmedRequestsCount(eventId);
            log.info("Завершено получение подтвержденных запросов через клиент для события, eventId={}", eventId);
            Set<Long> eventIds = Set.of(eventId);
            log.info("Получение комментариев через клиент для события, eventId={}", eventId);
            commentDtoList = commentClientInternal.getEventIdToCommentsDtoMap(eventIds).get(eventId);
            log.info("Завершено получение комментариев через клиент для события, eventId={}", eventId);
        }
        log.info("Получение пользователя через клиент для события, eventId={}", eventId);
        UserShortDto userShortDto = userClientInternal.getUserShortDtoById(event.getInitiatorId());
        log.info("Завершено получение пользователя через клиент для события, eventId={}", eventId);
        log.info("Получение категории через клиент для события, eventId={}", eventId);
        CategoryDto categoryDto = categoryClientInternal.getCategory(event.getCategoryId());
        log.info("Завершено получение категории через клиент для события, eventId={}", eventId);
        EventFullDto eventFullDto = EventMapper.mapToFullDto(event, userShortDto, categoryDto, views, confirmedRequests, commentDtoList);
        log.info("Завершено создание EventFullDto для события, eventId={}", event.getId());
        return eventFullDto;
    }

    public List<EventFullDto> getEventFullDtoList(Set<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> eventIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Event event : events) {
            eventIds.add(event.getId());
            categoryIds.add(event.getCategoryId());
            userIds.add(event.getInitiatorId());
        }
        Map<Long, UserShortDto> userIdToUserShortDtoMap = userClientInternal.userIdToUserShortDtoMap(userIds);
        Map<Long, CategoryDto> categoryIdToCategoryDtoMap = categoryClientInternal.getCategoryIdToCategoryDtoMap(categoryIds);
        Map<Long, Long> eventIdToViewsCountMap = getEventIdToViewsCountMap(events);
        Map<Long, Long> eventIdToConfirmedRequestsCountMap = participationClientInternal.getEventIdToConfirmedRequestsCount(eventIds);
        Map<Long, List<CommentDto>> eventIdToCommentsDtoMap = commentClientInternal.getEventIdToCommentsDtoMap(eventIds);

        return events.stream()
                .map(event -> EventMapper.mapToFullDto(event,
                        userIdToUserShortDtoMap.get(event.getInitiatorId()),
                        categoryIdToCategoryDtoMap.get(event.getCategoryId()),
                        eventIdToViewsCountMap.getOrDefault(event.getId(), 0L),
                        eventIdToConfirmedRequestsCountMap.getOrDefault(event.getId(), 0L),
                        eventIdToCommentsDtoMap.getOrDefault(event.getId(), Collections.emptyList())
                ))
                .sorted(Comparator.comparing(EventFullDto::getId))
                .toList();
    }

    public List<EventShortDto> getEventShortDtoList(Set<Event> events, boolean onlyAvailable) {
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> eventIds = new HashSet<>();
        Set<Long> categoryIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        for (Event event : events) {
            eventIds.add(event.getId());
            categoryIds.add(event.getCategoryId());
            userIds.add(event.getInitiatorId());
        }
        Map<Long, UserShortDto> userIdToUserShortDtoMap = userClientInternal.userIdToUserShortDtoMap(userIds);
        Map<Long, CategoryDto> categoryIdToCategoryDtoMap = categoryClientInternal.getCategoryIdToCategoryDtoMap(categoryIds);
        Map<Long, Long> eventIdToViewsCountMap = getEventIdToViewsCountMap(events);
        Map<Long, Long> eventIdToConfirmedRequestsCountMap = participationClientInternal.getEventIdToConfirmedRequestsCount(eventIds);
        Map<Long, List<CommentDto>> eventIdToCommentsDtoMap = commentClientInternal.getEventIdToCommentsDtoMap(eventIds);

        List<EventShortDto> dtoList = new ArrayList<>();
        for (Event event : events) {
            long confirmedRequestsCount = eventIdToConfirmedRequestsCountMap.getOrDefault(event.getId(), 0L);
            if (onlyAvailable && event.getParticipantLimit() > 0 && confirmedRequestsCount == event.getParticipantLimit()) {
                continue;
            }
            EventShortDto eventShortDto = EventMapper.mapToShortDto(event,
                    userIdToUserShortDtoMap.get(event.getInitiatorId()),
                    categoryIdToCategoryDtoMap.get(event.getCategoryId()),
                    eventIdToViewsCountMap.getOrDefault(event.getId(), 0L),
                    confirmedRequestsCount,
                    eventIdToCommentsDtoMap.getOrDefault(event.getId(), Collections.emptyList()));
            dtoList.add(eventShortDto);
        }
        return dtoList.stream()
                .sorted(Comparator.comparing(EventShortDto::getId))
                .toList();
    }

    @Transactional
    public Event updateEventFieldsFromUserRequest(UpdateEventUserRequest request, Event event) {
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

        if (request.getCategory() != null) {
            event.setCategoryId(request.getCategory());
        }

        if (request.getLocation() != null && request.getLocation().getLat() != null
            && request.getLocation().getLon() != null) {
            event.getLocation().setLat(request.getLocation().getLat());
            event.getLocation().setLon(request.getLocation().getLon());
        }
        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEventFieldsFromAdminRequest(UpdateEventAdminRequest request, Long eventId) {
        Event event = getEvent(eventId);

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

        if (request.getCategory() != null) {
            event.setCategoryId(request.getCategory());
        }

        if (request.getLocation() != null) {
            event.setLocation(locationMapper.mapLocationToEventLocation(request.getLocation()));
        }
        return eventRepository.save(event);
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
            booleanBuilder.and(event.categoryId.in(req.getCategories()));
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
            booleanBuilder.and(event.initiatorId.in(req.getUsers()));
        }
        if (req.getStates() != null && !req.getStates().isEmpty()) {
            booleanBuilder.and(event.state.in(req.getStates()));
        }
        if (req.getCategories() != null && !req.getCategories().isEmpty()) {
            booleanBuilder.and(event.categoryId.in(req.getCategories()));
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
