package ru.practicum.service;

import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CategoryService {
    CategoryDto addCategory(NewCategoryDto newCategoryDto);

    void deleteCategory(Long catId);

    CategoryDto updateCategory(Long catId, CategoryDto categoryDto);

    List<CategoryDto> getCategories(Integer from, Integer size);

    CategoryDto getCategory(Long catId);

    Map<Long, CategoryDto> getCategoryIdToCategoryDtoMap(@RequestBody Set<Long> categoryIds);
}
