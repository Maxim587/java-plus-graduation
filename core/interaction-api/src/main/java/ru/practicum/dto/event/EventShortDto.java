package ru.practicum.dto.event;

import lombok.Data;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.ArrayList;
import java.util.List;

@Data
public class EventShortDto {
    private String annotation;
    private CategoryDto category;
    private Long confirmedRequests;
    private String eventDate;
    private Long id;
    private UserShortDto initiator;
    private Boolean paid;
    private String title;
    private Double rating;
    private List<CommentDto> comments = new ArrayList<>();
}
