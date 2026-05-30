package ru.practicum.feign.common.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.event.admin.CompilationClientAdmin;
import ru.practicum.feign.common.nonauthorized.CompilationClientNonauthorized;

import java.util.List;

@Slf4j
@Component
public class CompilationClientFallback implements CompilationClientAdmin, CompilationClientNonauthorized {

    @Override
    public void delete(Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CompilationDto createCompilation(NewCompilationDto dto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest dto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: event service is unavailable");
    }
}
