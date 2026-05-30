package ru.practicum.feign.common.category;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.feign.common.fallback.CategoryFallback;
import ru.practicum.feign.config.FeignCustomConfig;

@FeignClient(
        name = "category-service-admin",
        url = "http://localhost:8080",
        path = "/admin/categories",
        fallback = CategoryFallback.class,
        configuration = FeignCustomConfig.class)
public interface CategoryClientAdmin {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryDto addCategory(@RequestBody @Valid NewCategoryDto newCategoryDto);

    @DeleteMapping("/{catId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long catId);

    @PatchMapping("/{catId}")
    CategoryDto updateCategory(@PathVariable Long catId,
                               @RequestBody @Valid CategoryDto categoryDto);
}