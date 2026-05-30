package ru.practicum.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.feign.internal.UserClientInternal;
import ru.practicum.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/users")
public class UserControllerInternal implements UserClientInternal {
    private final UserService userService;

    @Override
    @GetMapping("/{userId}")
    public UserShortDto getUserShortDtoById(@PathVariable Long userId) {
        log.info("Получен запрос на получение UserShortDto, userId={}", userId);
        return userService.getUserShortDtoById(userId);
    }

    @Override
    @GetMapping
    public List<UserShortDto> getUserShortDtos(@RequestBody Set<Long> userIds) {
        log.info("Получен запрос на получение списка UserShortDto, userIds={}", userIds);
        return userService.getUserShortDtos(userIds);
    }

    @Override
    public Map<Long, UserShortDto> userIdToUserShortDtoMap(Set<Long> userIds) {
        log.info("Получен запрос на получение словаря UserShortDto, userIds={}", userIds);
        return userService.userIdToUserShortDtoMap(userIds);
    }
}
