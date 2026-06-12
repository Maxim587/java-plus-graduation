package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.enums.ParticipationRequestStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {
    Optional<ParticipationRequest> getByIdAndRequesterId(Long requestId, Long userId);

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByIdIn(Collection<Long> ids);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    int countByEventIdAndStatus(Long eventId, ParticipationRequestStatus requestStatus);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    @Modifying
    @Query("update ParticipationRequest r set r.status = :status where r.id in :ids")
    void updateStatus(ParticipationRequestStatus status, List<Long> ids);

    @Query("select r.eventId, count(r.id) from ParticipationRequest r where r.status = :status and r.eventId in :ids group by r.eventId")
    List<Object[]> getEventIdToConfirmedRequestsCount(Set<Long> ids, ParticipationRequestStatus status);

    boolean existsByRequesterId(Long requesterId);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, ParticipationRequestStatus status);
}
