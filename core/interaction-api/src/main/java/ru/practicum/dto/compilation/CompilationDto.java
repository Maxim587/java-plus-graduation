package ru.practicum.dto.compilation;

import lombok.Data;
import ru.practicum.dto.event.EventShortDto;

import java.util.Set;

@Data
public class CompilationDto {
    private Set<EventShortDto> events;
    private Long id;
    private Boolean pinned;
    private String title;
}
