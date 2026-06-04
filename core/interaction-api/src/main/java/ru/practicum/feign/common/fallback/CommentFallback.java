package ru.practicum.feign.common.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.comment.*;
import ru.practicum.exception.FeignClientUnavailableException;
import ru.practicum.feign.common.comment.CommentClientAdmin;
import ru.practicum.feign.common.comment.CommentClientAuthorized;

import java.util.List;

@Slf4j
@Component
public class CommentFallback implements CommentClientAdmin, CommentClientAuthorized {
    @Override
    public List<CommentDtoAdmin> getComments(CommentSearchRequestAdmin param) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public List<CommentDtoAdmin> changeCommentStatus(CommentStatusChangeRequest dto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public void deleteCommentByAdmin(Long commentId) {
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CommentDtoAdmin getCommentById(Long commentId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto newDto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newDto) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    @Override
    public void deleteCommentByUser(Long userId, Long commentId) {
        logError();
        throw new FeignClientUnavailableException("Сервис временно недоступен");
    }

    void logError() {
        log.error("Fallback response: comment service is unavailable");
    }
}
