package ru.practicum.feign.common.category;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.common.fallback.CategoryFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.util.List;

@FeignClient(
        name = "category-service-nonauthorized",
        url = "http://localhost:8080",
        path = "/categories",
        fallback = CategoryFallback.class,
        configuration = FeignCustomConfig.class)
public interface CategoryClientNonauthorized {
    @GetMapping
    List<CategoryDto> getCategories(@RequestParam(required = false, defaultValue = "0") Integer from,
                                    @RequestParam(required = false, defaultValue = "10") Integer size);

    @GetMapping("/{catId}")
    CategoryDto getCategory(@PathVariable Long catId);
}