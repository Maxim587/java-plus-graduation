package ru.practicum.repositoty;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.EventSimilarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(long eventA, long eventB);

    List<EventSimilarity> findAllByEventAOrEventB(long eventA, long eventB);


    @Query("SELECT s FROM EventSimilarity s " +
           "WHERE (s.eventA IN :eventIds AND s.eventB NOT IN :allEventIds) " +
           "OR (s.eventB IN :eventIds AND s.eventA NOT IN :allEventIds)")
    List<EventSimilarity> findSimilarEvents(List<Long> eventIds, Set<Long> allEventIds, Pageable pageable);


    @Query(" SELECT s FROM EventSimilarity s " +
           "WHERE (s.eventA IN :newEventIds AND s.eventB IN :allUserEventIds) " +
           "OR (s.eventB IN :newEventIds AND s.eventA IN :allUserEventIds)")
    List<EventSimilarity> findNeighbours(Set<Long> newEventIds, Set<Long> allUserEventIds, Pageable pageable);

}
