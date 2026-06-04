package ru.practicum.feign.internal.fallback;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.internal.CommentClientInternal;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class CommentClientFallbackInternal implements CommentClientInternal {

    @Override
    public Map<Long, List<CommentDto>> getEventIdToCommentsDtoMap(Set<Long> eventIds) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public boolean existsByAuthorIdInternal(Long authorId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: comment service is unavailable");
    }
}
