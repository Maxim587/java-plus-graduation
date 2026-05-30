package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.feign.common.event.admin.CompilationClientAdmin;
import ru.practicum.service.CompilationService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/admin/compilations")
public class AdminCompilationsController implements CompilationClientAdmin {
    private final CompilationService service;

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long compId) {
        log.info("Получен запрос на удаление подборки Администратором, catId={}", compId);
        service.deleteCompilation(compId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@Valid @RequestBody NewCompilationDto dto) {
        log.info("Получен запрос на создание подборки Администратором {}", dto);

        return service.saveCompilation(dto);
    }

    @PatchMapping("/{compId}")
    @ResponseStatus(HttpStatus.OK)
    public CompilationDto updateCompilation(@PathVariable Long compId,
                                            @Valid @RequestBody UpdateCompilationRequest dto) {
        log.info("Получен запрос на обновление подборки Администратором {}, compId={}", dto, compId);
        return service.updateCompilation(compId, dto);
    }
}
