package ru.practicum.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.internal.CategoryClientInternal;
import ru.practicum.service.CategoryService;

import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/categories")
public class InternalCategoryController implements CategoryClientInternal {
    private final CategoryService categoryService;

    @GetMapping("/{categoryId}")
    public CategoryDto getCategory(@PathVariable Long categoryId) {
        log.info("Получен запрос на получение категории, categoryId={}", categoryId);
        return categoryService.getCategory(categoryId);
    }

    @GetMapping
    public Map<Long, CategoryDto> getCategoryIdToCategoryDtoMap(@RequestBody Set<Long> categoryIds) {
        log.info("Получен запрос на получение словаря категорий, categoryIds={}", categoryIds);
        return categoryService.getCategoryIdToCategoryDtoMap(categoryIds);
    }
}
