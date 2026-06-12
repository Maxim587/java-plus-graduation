package ru.practicum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    Optional<Event> findByIdAndInitiatorId(Long eventId, Long userId);

    Page<Event> findByInitiatorId(Long userId, Pageable pageable);

    List<Event> findAllByIdIn(Set<Long> eventIds);

    Optional<Event> getFirstByCategoryIdOrInitiatorId(Long categoryId, Long initiatorId);
}
