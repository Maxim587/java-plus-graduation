package ru.practicum.feign.common.nonauthorized;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.feign.common.fallback.EventClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(
        name = "event-service-nonauthorized",
        url = "http://localhost:8080",
        path = "/events",
        fallback = EventClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface EventClientNonauthorized {

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getEvent(@PathVariable Long id, HttpServletRequest request);


    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventShortDto> searchForUser(@RequestParam(required = false) String text,
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
                                      HttpServletRequest request);
}