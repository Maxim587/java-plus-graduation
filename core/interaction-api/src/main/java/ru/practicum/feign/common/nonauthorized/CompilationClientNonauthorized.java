package ru.practicum.feign.common.nonauthorized;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.feign.common.fallback.CompilationClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.util.List;

@FeignClient(
        name = "event-service-compilation-nonauthorized",
        url = "http://localhost:8080",
        path = "/compilations",
        fallback = CompilationClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface CompilationClientNonauthorized {

    @GetMapping
    List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                         @RequestParam(defaultValue = "0") Integer from,
                                         @RequestParam(defaultValue = "10") Integer size);

    @GetMapping("/{compId}")
    CompilationDto getCompilation(@PathVariable Long compId);
}