package ru.practicum.feign.common.event.admin;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.feign.common.fallback.CompilationClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

@FeignClient(
        name = "event-service-compilation-admin",
        url = "http://localhost:8080",
        path = "/admin/compilations",
        fallback = CompilationClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface CompilationClientAdmin {

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long compId);

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto dto);

    @PatchMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    CompilationDto updateCompilation(@PathVariable Long compId,
                                     @Valid @RequestBody UpdateCompilationRequest dto);

}