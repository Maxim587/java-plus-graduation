package ru.practicum.controller.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.feign.internal.CommentClientInternal;
import ru.practicum.service.CommentService;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/comments")
public class InternalCommentController implements CommentClientInternal {
    private final CommentService commentService;

    @Override
    @GetMapping("/map")
    public Map<Long, List<CommentDto>> getEventIdToCommentsDtoMap(@RequestBody Set<Long> eventIds) {
        log.info("Получен запрос на формирование словаря комментариев по событиям" + eventIds);
        return commentService.getEventIdToCommentsDtoMap(eventIds);
    }

    @Override
    @GetMapping("/exists")
    public boolean existsByAuthorIdInternal(@RequestParam Long authorId) {
        return commentService.existsByAuthorIdInternal(authorId);
    }
}
