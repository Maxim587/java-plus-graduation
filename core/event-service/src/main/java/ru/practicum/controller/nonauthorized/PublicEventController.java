package ru.practicum.controller.nonauthorized;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.NewEndpointHitDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventSearchRequestUser;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.StatsClient;
import ru.practicum.feign.common.nonauthorized.EventClientNonauthorized;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

import static ru.practicum.service.EventServiceImpl.DATE_TIME_FORMATTER;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/events")
public class PublicEventController implements EventClientNonauthorized {
    private static final String APP_NAME = "main-service";
    private final EventService eventService;
    private final StatsClient statsClient;

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request) {
        log.info("Получен запрос на получение информации о событии {}", id);

        NewEndpointHitDto hitDto = new NewEndpointHitDto(
                APP_NAME,
                request.getRequestURI(),
                request.getRemoteAddr(),
                LocalDateTime.now().format(DATE_TIME_FORMATTER)
        );

        try {
            log.info("Добавление события getEvent в сервис статистики с dto={}", hitDto);
            statsClient.hit(hitDto);
            log.info("Добавление события getEvent в сервис статистики завершено успешно");
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
        log.info("Сформирован DTO с параметрами запроса {}", param);

        List<EventShortDto> resp = eventService.searchByUser(param);

        NewEndpointHitDto hitDto = new NewEndpointHitDto(APP_NAME, request.getRequestURI(),
                request.getRemoteAddr(), LocalDateTime.now().format(DATE_TIME_FORMATTER));

        try {
            log.info("Добавление события в сервис статистики с dto={}", hitDto);
            statsClient.hit(hitDto);
            log.info("Добавление события в сервис статистики завершено успешно");
            return resp;
        } catch (FeignException e) {
            log.error("Ошибка feign-клиента сервиса статистики: {}", e.getMessage());
            throw new FeignClientUnavailableException(e.getMessage());
        }
    }
}
