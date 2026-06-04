package ru.practicum.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.EndpointHitDto;
import ru.practicum.NewEndpointHitDto;
import ru.practicum.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;


@Slf4j
@Component
public class StatsClientFallback implements StatsClient {

    @Override
    public EndpointHitDto hit(NewEndpointHitDto newEndpointHitDto) {
        log.error("Fallback response: stats server is unavailable");
        return null;
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, Boolean unique) {
        log.error("Fallback response: stats server is unavailable");
        return Collections.emptyList();
    }
}
