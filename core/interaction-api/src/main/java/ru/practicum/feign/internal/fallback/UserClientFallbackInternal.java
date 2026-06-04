package ru.practicum.feign.internal.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.internal.UserClientInternal;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class UserClientFallbackInternal implements UserClientInternal {

    @Override
    public UserShortDto getUserShortDtoById(Long userId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<UserShortDto> getUserShortDtos(Set<Long> userIds) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public Map<Long, UserShortDto> userIdToUserShortDtoMap(@RequestBody Set<Long> userIds) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: user service is unavailable");
    }
}
