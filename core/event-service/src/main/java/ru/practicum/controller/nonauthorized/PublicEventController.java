package ru.practicum.controller.nonauthorized;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventSearchRequestUser;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.enums.UserActionType;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.nonauthorized.EventClientNonauthorized;
import ru.practicum.grpc.CollectorGrpcClient;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.header.Headers.USER_ID_HEADER;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class PublicEventController implements EventClientNonauthorized {
    private final EventService eventService;
    private final CollectorGrpcClient collectorGrpcClient;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long id,
                                 @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Получен запрос на получение информации о событии {}", id);

        try {
            log.info("Добавление информации о просмотре события id={} пользователем id={} в сервис статистики", id, userId);
            collectorGrpcClient.collectUserAction(id, userId, UserActionType.ACTION_VIEW);
            log.info("Завершено добавление информации о просмотре события id={} пользователем id={} в сервис статистики", id, userId);
            return eventService.getPublicEvent(id);
        } catch (FeignException e) {
            log.error("Ошибка feign-клиента сервиса статистики: {}", e.getMessage());
            throw new FeignClientUnavailableException(e.getMessage());
        }
    }


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventShortDto> searchForUser(@RequestParam(required = false) String text,
                                             @RequestParam(required = false) List<Long> categories,
                                             @RequestParam(required = false) Boolean paid,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
                                             @RequestParam(required = false)
                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
                                             @RequestParam(defaultValue = "false") Boolean onlyAvailable,
                                             @RequestParam(required = false) String sort,
                                             @RequestParam(defaultValue = "0") Integer from,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             HttpServletRequest request) {
        log.info("Получен запрос на получение событий неавторизованным пользователем");
        EventSearchRequestUser param = new EventSearchRequestUser(text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);
        List<EventShortDto> resp = eventService.searchByUser(param);
        log.info("Направлен ответ на запрос на получение событий неавторизованным пользователем. Количество событий в ответе: {}", resp.size());
        return resp;
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestParam Integer maxResults,
                                                  @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Получен запрос на получение рекомендаций");
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable Long eventId,
                          @RequestHeader(USER_ID_HEADER) Long userId) {
        log.info("Получен запрос на добавление лайка для события");
        eventService.likeEvent(eventId, userId);
    }
}
