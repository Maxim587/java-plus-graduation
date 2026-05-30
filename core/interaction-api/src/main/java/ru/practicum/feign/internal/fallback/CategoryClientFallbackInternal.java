package ru.practicum.feign.internal.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.internal.CategoryClientInternal;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CategoryClientFallbackInternal implements CategoryClientInternal {

    @Override
    public CategoryDto getCategory(Long categoryId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public Map<Long, CategoryDto> getCategoryIdToCategoryDtoMap(@RequestBody Set<Long> categoryIds) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: category service is unavailable");
    }
}
