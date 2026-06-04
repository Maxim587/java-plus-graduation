package ru.practicum.feign.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.feign.config.FeignCustomConfig;
import ru.practicum.feign.internal.fallback.EventClientFallbackInternal;

@FeignClient(
        name = "event-service-internal",
        url = "http://localhost:8080",
        path = "/internal/events",
        fallback = EventClientFallbackInternal.class,
        configuration = FeignCustomConfig.class)
public interface EventClientInternal {

    @GetMapping("/{eventId}")
    EventInternalDto getEventByIdInternal(@PathVariable Long eventId);

    @GetMapping
    EventInternalDto getExistingEventInternal(@RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) Long initiatorId);
}