package ru.practicum.dto.event;

import lombok.Data;
import ru.practicum.dto.location.Location;
import ru.practicum.enums.EventState;

import java.time.LocalDateTime;

@Data
public class EventInternalDto {
    private Long id;
    private String annotation;
    private Long categoryId;
    private LocalDateTime createdOn;
    private String description;
    private LocalDateTime eventDate;
    private Long initiatorId;
    private boolean paid;
    private Integer participantLimit;
    private LocalDateTime publishedOn;
    private Boolean requestModeration;
    private EventState state;
    private String title;
    private Location location;
}
