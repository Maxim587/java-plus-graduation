package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private final CompilationRepository repository;
    private final EventRepository eventRepository;
    private final CompilationMapper mapper;
    private final EventServiceHelper eventHelper;

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        if (!repository.existsById(compId)) {
            throw new NotFoundException("Подборка с id " + compId + " не найдена");
        }
        repository.deleteById(compId);
    }

    @Override
    @Transactional
    public CompilationDto saveCompilation(NewCompilationDto dto) {
        Set<Event> events = getEvents(dto.getEvents());
        Compilation compilation = mapper.mapNewCompilationDtoToCompilation(dto, events);
        compilation = repository.save(compilation);
        List<EventShortDto> getEventShortDtoList = eventHelper.getEventShortDtoList(events,false);
        return mapper.mapCompilationToCompilationDto(compilation, getEventShortDtoList);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        Compilation compilation = findCompilationById(compId);
        updateCompilationFields(compilation, updateRequest);
        compilation = repository.save(compilation);
        List<EventShortDto> getEventShortDtoList = eventHelper.getEventShortDtoList(compilation.getEvents(), false);
        return mapper.mapCompilationToCompilationDto(compilation, getEventShortDtoList);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        List<Compilation> compilations = findCompilations(pinned, from, size);
        return convertCompilationsIntoDtos(compilations);
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        Compilation compilation = findCompilationById(compId);
        List<EventShortDto> eventShortDtoList = eventHelper.getEventShortDtoList(compilation.getEvents(), false);
        return mapper.mapCompilationToCompilationDto(compilation, eventShortDtoList);
    }

    private Set<Event> getEvents(Set<Long> eventIds) {
        Set<Event> events = new HashSet<>();
        if (eventIds != null && !eventIds.isEmpty()) {
            events.addAll(eventRepository.findAllByIdIn(eventIds));
            if (eventIds.size() > events.size()) {
                throw new NotFoundException("В переданном списке событий есть события, которые не найдены в системе");
            }
        }
        return events;
    }

    private void updateCompilationFields(Compilation compilation, UpdateCompilationRequest updateRequest) {
        if (updateRequest.getEvents() != null) {
            compilation.setEvents(getEvents(updateRequest.getEvents()));
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getTitle() != null) {
            compilation.setTitle(updateRequest.getTitle());
        }
    }

    private List<Compilation> findCompilations(Boolean pinned, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        if (pinned != null && pinned) {
            return repository.findAllByPinned(true, page);
        }
        return repository.getCompilationList(page);
    }

    private List<CompilationDto> convertCompilationsIntoDtos(List<Compilation> compilations) {
        Map<Long, Set<Event>> compilationIdToEventsMap = new HashMap<>();
        Map<Long, Compilation> compilationIdToCompilationMap = new HashMap<>();
        Set<Event> eventsAll = new HashSet<>();

        for (Compilation compilation : compilations) {
            compilationIdToEventsMap.put(compilation.getId(), compilation.getEvents());
            compilationIdToCompilationMap.put(compilation.getId(), compilation);
            eventsAll.addAll(compilation.getEvents());
        }

        List<EventShortDto> eventShortDtoList = eventHelper.getEventShortDtoList(eventsAll, false);
        Map<Long, EventShortDto> eventIdDtoMap = eventShortDtoList.stream()
                .collect(Collectors.toMap(EventShortDto::getId, Function.identity()));

        List<CompilationDto> compilationDtos = new ArrayList<>();

        for (Map.Entry<Long, Set<Event>> entry : compilationIdToEventsMap.entrySet()) {
            Compilation compilation = compilationIdToCompilationMap.get(entry.getKey());
            List<EventShortDto> shortDtos = entry.getValue().stream()
                    .map(event -> eventIdDtoMap.get(event.getId()))
                    .toList();
            compilationDtos.add(mapper.mapCompilationToCompilationDto(compilation, shortDtos));
        }
        return compilationDtos;
    }

    private Compilation findCompilationById(Long compId) {
        return repository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id " + compId + " не найдена"));
    }
}
