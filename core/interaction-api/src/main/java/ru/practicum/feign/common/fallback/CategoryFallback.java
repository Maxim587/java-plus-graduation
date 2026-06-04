package ru.practicum.feign.common.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.category.CategoryClientAdmin;
import ru.practicum.feign.common.category.CategoryClientNonauthorized;

import java.util.List;

@Slf4j
@Component
public class CategoryFallback implements CategoryClientAdmin, CategoryClientNonauthorized {

    @Override
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public void delete(Long catId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: category service is unavailable");
    }
}
