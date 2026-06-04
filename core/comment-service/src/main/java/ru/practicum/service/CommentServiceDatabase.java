package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.enums.CommentStatus;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.repository.CommentRepository;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceDatabase {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;

    @Transactional
    Comment saveCommentInDb(Long userId, Long eventId, NewCommentDto commentDto) {
        log.info("Сохранение комментария в БД для eventId={}", eventId);
        return commentRepository.save(commentMapper.mapToComment(commentDto, userId, eventId));
    }

    @Transactional
    public CommentDto updateCommentInDb(Long userId, Long commentId, NewCommentDto commentDto, String name) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + commentId + " не найден"));
        checkUserIsCommentAuthor(userId, comment);
        comment.setText(commentDto.getText());
        comment.setStatus(CommentStatus.PENDING);
        comment = commentRepository.save(comment);
        return commentMapper.mapToCommentDto(comment, name);
    }

    public List<Comment> getComments(Set<Long> eventIds) {
        return commentRepository.findAllByEventIdIn(eventIds);
    }

    private void checkUserIsCommentAuthor(Long userId, Comment comment) {
        if (!Objects.equals(comment.getAuthorId(), userId)) {
            throw new ConditionsConflictException("Пользователь с id=" + userId + " не является автором комментария id=" + comment.getId());
        }
    }

}
