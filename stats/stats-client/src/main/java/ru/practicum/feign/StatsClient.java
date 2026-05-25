package ru.practicum.feign;


import feign.FeignException;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.EndpointHitDto;
import ru.practicum.NewEndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;


@FeignClient(name = "stats-server", fallback = StatsClientFallback.class)
public interface StatsClient {

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    EndpointHitDto hit(@RequestBody @Valid NewEndpointHitDto newEndpointHitDto) throws FeignException;

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                @RequestParam(required = false) List<String> uris,
                                @RequestParam(defaultValue = "false") Boolean unique
    ) throws FeignException;
}

