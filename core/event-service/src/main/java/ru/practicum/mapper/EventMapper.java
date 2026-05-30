package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import org.mapstruct.factory.Mappers;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventInternalDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.NewEventDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.EventState;
import ru.practicum.model.Event;
import ru.practicum.service.EventServiceImpl;

import java.time.LocalDateTime;
import java.util.List;


@UtilityClass
public class EventMapper {
    private final LocationMapper locationMapper = Mappers.getMapper(LocationMapper.class);

    public static Event mapToEvent(NewEventDto newEventDto, Long userId, LocalDateTime eventDate) {
        Event event = new Event();

        event.setAnnotation(newEventDto.getAnnotation());
        event.setDescription(newEventDto.getDescription());
        event.setEventDate(eventDate);
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiatorId(userId);
        event.setCategoryId(newEventDto.getCategory());
        event.setLocation(locationMapper.mapLocationToEventLocation(newEventDto.getLocation()));
        event.setPaid(newEventDto.getPaid());
        event.setParticipantLimit(newEventDto.getParticipantLimit());
        event.setRequestModeration(newEventDto.getRequestModeration());
        event.setState(EventState.PENDING);
        event.setTitle(newEventDto.getTitle());

        return event;
    }

    public static EventFullDto mapToFullDto(Event event,
                                            UserShortDto initiator,
                                            CategoryDto categoryDto,
                                            Long views,
                                            Long confirmedRequests,
                                            List<CommentDto> commentDtoList) {
        EventFullDto fullDto = new EventFullDto();
        fullDto.setAnnotation(event.getAnnotation());
        fullDto.setCategory(categoryDto);
        fullDto.setCreatedOn(EventServiceImpl.DATE_TIME_FORMATTER.format(event.getCreatedOn()));
        fullDto.setDescription(event.getDescription());
        fullDto.setEventDate(EventServiceImpl.DATE_TIME_FORMATTER.format(event.getEventDate()));
        fullDto.setId(event.getId());
        fullDto.setInitiator(initiator);
        fullDto.setLocation(locationMapper.mapEventLocationToLocation(event.getLocation()));
        fullDto.setPaid(event.isPaid());
        fullDto.setParticipantLimit(event.getParticipantLimit());
        fullDto.setPublishedOn(event.getPublishedOn() != null ? EventServiceImpl.DATE_TIME_FORMATTER.format(event.getPublishedOn()) : null);
        fullDto.setRequestModeration(event.getRequestModeration());
        fullDto.setState(event.getState());
        fullDto.setTitle(event.getTitle());
        fullDto.setViews(views);
        fullDto.setConfirmedRequests(confirmedRequests);
        fullDto.setComments(commentDtoList);
        return fullDto;
    }

    public static EventShortDto mapToShortDto(Event event,
                                              UserShortDto initiator,
                                              CategoryDto categoryDto,
                                              Long views,
                                              Long confirmedRequests,
                                              List<CommentDto> commentDtoList) {
        EventShortDto shortDto = new EventShortDto();

        shortDto.setAnnotation(event.getAnnotation());
        shortDto.setCategory(categoryDto);
        shortDto.setEventDate(EventServiceImpl.DATE_TIME_FORMATTER.format(event.getEventDate()));
        shortDto.setId(event.getId());
        shortDto.setInitiator(initiator);
        shortDto.setPaid(event.isPaid());
        shortDto.setTitle(event.getTitle());
        shortDto.setViews(views);
        shortDto.setConfirmedRequests(confirmedRequests);
        shortDto.setComments(commentDtoList);

        return shortDto;
    }

    public static EventInternalDto mapToInternalDto(Event event) {
        EventInternalDto eventInternalDto = new EventInternalDto();
        eventInternalDto.setId(event.getId());
        eventInternalDto.setAnnotation(event.getAnnotation());
        eventInternalDto.setCategoryId(event.getCategoryId());
        eventInternalDto.setCreatedOn(event.getCreatedOn());
        eventInternalDto.setDescription(event.getDescription());
        eventInternalDto.setEventDate(event.getEventDate());
        eventInternalDto.setInitiatorId(event.getInitiatorId());
        eventInternalDto.setPaid(event.isPaid());
        eventInternalDto.setParticipantLimit(event.getParticipantLimit());
        eventInternalDto.setPublishedOn(event.getPublishedOn());
        eventInternalDto.setRequestModeration(event.getRequestModeration());
        eventInternalDto.setState(event.getState());
        eventInternalDto.setTitle(event.getTitle());
        eventInternalDto.setLocation(locationMapper.mapEventLocationToLocation(event.getLocation()));
        return eventInternalDto;
    }
}
