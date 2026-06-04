package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.NotFoundException;
import ru.practicum.repository.CategoryRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceDatabase {
    private final CategoryRepository categoryRepository;

    @Transactional
    public void deleteCategoryInDatabase(Long catId) {
        log.info("Удаление категории из БД: catId={}", catId);
        categoryRepository.deleteById(catId);
        log.info("Завершено удаление категории из БД: catId={}", catId);
    }

    public void checkCategoryExists(Long catId) {
        log.info("Поиск категории в БД: catId={}", catId);
        categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Категория с id " + catId + " не найдена"));
        log.info("Поиск категории в БД завершен успешно: catId={}", catId);
    }
}
