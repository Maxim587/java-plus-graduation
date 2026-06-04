package ru.practicum.feign.common.event.admin;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.UpdateEventAdminRequest;
import ru.practicum.enums.EventState;
import ru.practicum.feign.common.fallback.EventClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
        name = "event-service-admin",
        url = "http://localhost:8080",
        path = "/admin/events",
        fallback = EventClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface EventClientAdmin {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventFullDto> searchForAdmin(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<EventState> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size);


    @PatchMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto updateByAdmin(@PathVariable Long eventId,
                               @Valid @RequestBody UpdateEventAdminRequest request);

}