package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventSearchRequestAdmin;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.enums.EventState;
import ru.practicum.feign.common.event.admin.EventClientAdmin;
import ru.practicum.service.EventService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/events")
public class AdminEventController implements EventClientAdmin {
    private final EventService eventService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<EventFullDto> searchForAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {

        log.info("Получен запрос на поиск события администратором");
        EventSearchRequestAdmin param = new EventSearchRequestAdmin(users, states, categories, rangeStart, rangeEnd, from, size);
        return eventService.searchByAdmin(param);
    }

    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public EventFullDto updateByAdmin(@PathVariable Long eventId,
                                      @Valid @RequestBody UpdateEventAdminRequest request) {
        log.info("Получен запрос на обновление события {} Администратором {}", eventId, request);
        return eventService.updateByAdmin(eventId, request);
    }
}
