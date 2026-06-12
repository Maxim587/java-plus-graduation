package ru.practicum.repositoty;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Optional;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(long userId, long eventId);

    List<Long> findEventIdsByUserIdAndEventIdIsNot(long userId, long eventId);

    List<Long> findAllByUserId(long userId, Pageable pageable);

    List<UserAction> findAllByUserId(long userId);

    List<UserAction> findAllByEventIdIn(List<Long> eventIds);
}
