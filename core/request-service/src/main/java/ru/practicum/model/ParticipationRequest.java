package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.enums.ParticipationRequestStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "participation_requests")
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private final LocalDateTime created = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

    private Long eventId;

    private Long requesterId;

    @Enumerated(EnumType.STRING)
    private ParticipationRequestStatus status = ParticipationRequestStatus.PENDING;
}
