package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.enums.CommentStatus;


import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at")
    private final LocalDateTime created = LocalDateTime.now();

    @Column(name = "text", length = 2000)
    private String text;

    private Long authorId;

    private Long eventId;

    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.PENDING;
}
