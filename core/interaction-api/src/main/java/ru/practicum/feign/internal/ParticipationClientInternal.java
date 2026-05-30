package ru.practicum.feign.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.feign.config.FeignCustomConfig;
import ru.practicum.feign.internal.fallback.ParticipationClientFallbackInternal;

import java.util.Map;
import java.util.Set;

@FeignClient(
        name = "participation-service-internal",
        url = "http://localhost:8080",
        path = "/internal/participation",
        fallback = ParticipationClientFallbackInternal.class,
        configuration = FeignCustomConfig.class)
public interface ParticipationClientInternal {

    @GetMapping("/{eventId}/confirmed")
    Long getConfirmedRequestsCount(@PathVariable Long eventId);

    @GetMapping("/confirmed")
    Map<Long, Long> getEventIdToConfirmedRequestsCount(@RequestBody Set<Long> eventIds);

    @GetMapping("/exists")
    boolean existsByRequesterIdInternal(@RequestParam Long requesterId);
}