package ru.practicum.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EventRequestStatusUpdateRequest {
    @NotNull
    private List<Long> requestIds;

    @NotBlank
    private String status;
}
