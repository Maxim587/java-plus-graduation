package ru.practicum.service;

import ru.practicum.dto.comment.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface CommentService {

    CommentDto createComment(Long userId, Long eventId, NewCommentDto commentDto);

    CommentDto updateComment(Long userId, Long commentId, NewCommentDto commentDto);

    List<CommentDtoAdmin> searchCommentsByAdmin(CommentSearchRequestAdmin param);

    List<CommentDtoAdmin> changeCommentStatus(CommentStatusChangeRequest dto);

    void deleteCommentByAdmin(Long commentId);

    CommentDtoAdmin getCommentById(Long commentId);

    void deleteCommentByUser(Long userId, Long commentId);

    Map<Long, List<CommentDto>> getEventIdToCommentsDtoMap(Set<Long> eventIds);

    boolean existsByAuthorIdInternal(Long authorId);
}
