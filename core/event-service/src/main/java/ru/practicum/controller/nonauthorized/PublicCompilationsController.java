package ru.practicum.controller.nonauthorized;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.feign.common.nonauthorized.CompilationClientNonauthorized;
import ru.practicum.service.CompilationService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/compilations")
public class PublicCompilationsController implements CompilationClientNonauthorized {
    private final CompilationService service;

    @GetMapping
    public List<CompilationDto> getCompilations(@RequestParam(required = false) Boolean pinned,
                                                @RequestParam(defaultValue = "0") Integer from,
                                                @RequestParam(defaultValue = "10") Integer size) {
        log.info("Получен запрос на получение списка подборок от неавторизованного пользователя, pinned={}, from={}, size={}", pinned, from, size);
        return service.getCompilations(pinned, from, size);
    }

    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId) {
        log.info("Получен запрос на получение подборки от неавторизованного пользователя, id={}", compId);
        return service.getCompilationById(compId);
    }
}
