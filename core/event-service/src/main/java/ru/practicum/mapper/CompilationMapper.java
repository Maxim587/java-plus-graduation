package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CompilationMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "events", source = "eventsSet")
    Compilation mapNewCompilationDtoToCompilation(NewCompilationDto newCompilationDto, Set<Event> eventsSet);

    @Mapping(target = "events", source = "eventShortDtoList")
    CompilationDto mapCompilationToCompilationDto(Compilation compilation, List<EventShortDto> eventShortDtoList);
}
