package ru.practicum.feign.common.user;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.feign.common.fallback.UserClientFallback;
import ru.practicum.feign.config.FeignCustomConfig;

import java.util.List;

@FeignClient(
        name = "user-service",
        path = "/admin/users",
        fallback = UserClientFallback.class,
        configuration = FeignCustomConfig.class)
public interface UserClient {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    UserDto registerUser(@RequestBody @Valid NewUserRequest newUser);


    @GetMapping
    List<UserDto> getUsers(@RequestParam(required = false) List<Long> ids,
                                  @RequestParam(required = false, defaultValue = "0") Integer from,
                                  @RequestParam(required = false, defaultValue = "10") Integer size);

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long userId);
}