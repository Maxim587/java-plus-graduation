package ru.practicum.feign.common.participation;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.dto.participation.ParticipationRequestDto;
import ru.practicum.feign.common.fallback.ParticipationClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.util.List;


@FeignClient(
        name = "request-service",
        fallback = ParticipationClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface ParticipationClient {

    @PostMapping("/users/{userId}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    ParticipationRequestDto addParticipationRequest(@PathVariable Long userId,
                                                    @RequestParam Long eventId);

    @GetMapping("/users/{userId}/requests")
    List<ParticipationRequestDto> getUserRequests(@PathVariable Long userId);

    @PatchMapping("/users/{userId}/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    ParticipationRequestDto cancelRequest(@PathVariable Long userId,
                                          @PathVariable Long requestId);


    @PatchMapping("/users/{userId}/events/{eventId}/requests")
    @ResponseStatus(HttpStatus.OK)
    EventRequestStatusUpdateResult changeRequestStatus(@PathVariable Long userId,
                                                       @PathVariable Long eventId,
                                                       @Valid @RequestBody EventRequestStatusUpdateRequest dto);


    @GetMapping("/users/{userId}/events/{eventId}/requests")
    List<ParticipationRequestDto> getEventParticipants(@PathVariable Long userId,
                                                       @PathVariable Long eventId);
}
