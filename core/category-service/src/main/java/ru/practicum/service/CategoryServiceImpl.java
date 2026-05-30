package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.internal.EventClientInternal;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;
    private final EventClientInternal eventClientInternal;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        return mapper.mapCategoryToCategoryDto(categoryRepository.save(mapper.mapNewCategoryDtoToCategory(newCategoryDto)));
    }

    @Override
    @Transactional
    public void deleteCategory(Long catId) {
        categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        EventInternalDto event = eventClientInternal.getExistingEventInternal(catId, null);
        if (event != null) {
            throw new ConditionsConflictException("Невозможно удалить категорию id=" + catId + ", т.к. есть связанное событие id=" + event.getId());
        }
        categoryRepository.deleteById(catId);
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long catId, CategoryDto categoryDto) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));

        category.setName(categoryDto.getName());

        return mapper.mapCategoryToCategoryDto(categoryRepository.save(category));
    }

    @Override
    public List<CategoryDto> getCategories(Integer from, Integer size) {
        PageRequest page = PageRequest.of(from / size, size);
        return categoryRepository.findAll(page).stream()
                .map(mapper::mapCategoryToCategoryDto)
                .toList();
    }

    @Override
    public CategoryDto getCategory(Long catId) {
        Category category = categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        return mapper.mapCategoryToCategoryDto(category);
    }

    @Override
    public Map<Long, CategoryDto> getCategoryIdToCategoryDtoMap(@RequestBody Set<Long> categoryIds) {
        List<Category> categories = categoryRepository.findAllByIdIn(categoryIds);
        return categories.stream()
                .collect(Collectors.toMap(Category::getId, mapper::mapCategoryToCategoryDto));
    }
}
