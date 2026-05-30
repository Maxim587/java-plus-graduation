package ru.practicum.feign.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.config.FeignCustomConfig;
import ru.practicum.feign.internal.fallback.CategoryClientFallbackInternal;

import java.util.Map;
import java.util.Set;

@FeignClient(
        name = "category-service-internal",
        url = "http://localhost:8080",
        path = "/internal/categories",
        fallback = CategoryClientFallbackInternal.class,
        configuration = FeignCustomConfig.class)
public interface CategoryClientInternal {

    @GetMapping("/{categoryId}")
    CategoryDto getCategory(@PathVariable Long categoryId);

    @GetMapping
    Map<Long, CategoryDto> getCategoryIdToCategoryDtoMap(@RequestBody Set<Long> categoryIds);

}