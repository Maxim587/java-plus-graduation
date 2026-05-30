package ru.practicum.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserService {
    UserDto registerUser(NewUserRequest newUser);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);

    UserShortDto getUserShortDtoById(Long userId);

    List<UserShortDto> getUserShortDtos(Set<Long> userIds);

    Map<Long, UserShortDto> userIdToUserShortDtoMap(Set<Long> userIds);
}
