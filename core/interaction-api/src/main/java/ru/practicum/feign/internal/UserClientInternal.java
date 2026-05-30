package ru.practicum.feign.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.feign.config.FeignCustomConfig;
import ru.practicum.feign.internal.fallback.UserClientFallbackInternal;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(
        name = "user-service-internal",
        url = "http://localhost:8080",
        path = "/internal/users",
        fallback = UserClientFallbackInternal.class,
        configuration = FeignCustomConfig.class)
public interface UserClientInternal {

    @GetMapping("/{userId}")
    UserShortDto getUserShortDtoById(@PathVariable Long userId);

    @GetMapping
    List<UserShortDto> getUserShortDtos(@RequestBody Set<Long> userIds);

    @GetMapping("/map")
    Map<Long, UserShortDto> userIdToUserShortDtoMap(@RequestBody Set<Long> userIds);
}