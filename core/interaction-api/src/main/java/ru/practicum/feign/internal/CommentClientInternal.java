package ru.practicum.feign.internal;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.feign.config.FeignCustomConfig;
import ru.practicum.feign.internal.fallback.CommentClientFallbackInternal;

import java.util.List;
import java.util.Map;
import java.util.Set;

@FeignClient(
        name = "comment-service-internal",
        url = "http://localhost:8080",
        path = "/internal/comments",
        fallback = CommentClientFallbackInternal.class,
        configuration = FeignCustomConfig.class)
public interface CommentClientInternal {

    @GetMapping("/map")
    Map<Long, List<CommentDto>> getEventIdToCommentsDtoMap(@RequestBody Set<Long> eventIds);

    @GetMapping("/exists")
    boolean existsByAuthorIdInternal(@RequestParam Long authorId);
}