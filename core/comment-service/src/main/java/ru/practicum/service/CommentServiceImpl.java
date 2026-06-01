package ru.practicum.service;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.CommentStatus;
import ru.practicum.exception.ConditionsConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.internal.UserClientInternal;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.QComment;
import ru.practicum.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final UserClientInternal userClientInternal;


    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto commentDto) {
        UserShortDto userShortDto = getUserShortDto(userId);
        Comment comment = commentRepository.save(commentMapper.mapToComment(commentDto, userShortDto.getId(), eventId));
        return commentMapper.mapToCommentDto(comment, userShortDto.getName());
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto commentDto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + commentId + " не найден"));
        checkUserIsCommentAuthor(userId, comment);
        comment.setText(commentDto.getText());
        comment.setStatus(CommentStatus.PENDING);
        UserShortDto userShortDto = getUserShortDto(userId);
        comment = commentRepository.save(comment);
        return commentMapper.mapToCommentDto(comment, userShortDto.getName());
    }

    @Override
    public List<CommentDtoAdmin> searchCommentsByAdmin(CommentSearchRequestAdmin param) {
        checkDates(param.getRangeStart(), param.getRangeEnd());
        Optional<Predicate> searchCriteriaOpt = getAdminCommentSearchCriteria(param);
        PageRequest page = PageRequest.of(param.getFrom() / param.getSize(), param.getSize());

        return searchCriteriaOpt.map(predicate -> commentRepository.findAll(predicate, page).getContent())
                .orElseGet(() -> commentRepository.findAll(page).getContent())
                .stream()
                .map(commentMapper::mapToCommentDtoAdmin)
                .toList();
    }

    @Override
    @Transactional
    public List<CommentDtoAdmin> changeCommentStatus(CommentStatusChangeRequest dto) {
        CommentStatus status = CommentStatus.fromString(dto.getStatus());
        if (status != CommentStatus.CONFIRMED && status != CommentStatus.REJECTED) {
            throw new ConditionsConflictException("Комментарий можно перевести в CONFIRMED или REJECTED. Передан статус " + status);
        }
        commentRepository.updateStatus(status, dto.getCommentIds());
        return commentRepository.findAllByIdIn(dto.getCommentIds()).stream()
                .map(commentMapper::mapToCommentDtoAdmin)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentDtoAdmin getCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .map(commentMapper::mapToCommentDtoAdmin)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + commentId + " не найден"));
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + commentId + " не найден"));
        checkUserIsCommentAuthor(userId, comment);
        commentRepository.deleteById(commentId);
    }

    @Override
    public Map<Long, List<CommentDto>> getEventIdToCommentsDtoMap(Set<Long> eventIds) {
        log.info("Начало формирования словаря EventIdToCommentsDto для событий " + eventIds);
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Comment> comments = commentRepository.findAllByEventIdIn(eventIds);
        if (comments == null || comments.isEmpty()) {
            log.info("Комментарии для событий не найдены. Возвращается пустой словарь");
            return Collections.emptyMap();
        }
        log.info("Найдены комментарии для событий в количестве: {}", comments.size());
        Set<Long> userIds = comments.stream().map(Comment::getAuthorId).collect(Collectors.toSet());
        log.info("Запрос в user-service");
        List<UserShortDto> userShortDtos = getUserShortDto(userIds);
        log.info("Запрос в user-service завершен. Получен список DTO в количестве: {}", userShortDtos.size());
        Map<Long, String> userNames = userShortDtos.stream().collect(Collectors.toMap(UserShortDto::getId, UserShortDto::getName));

        Map<Long, List<CommentDto>> commentsMap = new HashMap<>();
        comments.forEach(comment -> {
            commentsMap.computeIfAbsent(comment.getEventId(), eventId -> new ArrayList<>())
                    .add(commentMapper.mapToCommentDto(comment, userNames.get(comment.getAuthorId())));
        });
        log.info("Сформирован словарь EventIdToCommentsDto: {}", commentsMap.size());
        return commentsMap;
    }

    @Override
    public boolean existsByAuthorIdInternal(Long authorId) {
        return commentRepository.existsByAuthorId(authorId);
    }

    private static void checkDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new ValidationException("Дата начала не может быть позже даты окончания.");
        }
    }

    private Optional<Predicate> getAdminCommentSearchCriteria(CommentSearchRequestAdmin req) {
        QComment comment = QComment.comment;
        BooleanBuilder booleanBuilder = new BooleanBuilder();

        Optional.ofNullable(req.getText()).filter(text -> !text.isBlank())
                .ifPresent(text -> booleanBuilder.and(comment.text.containsIgnoreCase(text)));
        Optional.ofNullable(req.getEventIds()).filter(eventIds -> !eventIds.isEmpty())
                .ifPresent(eventIds -> booleanBuilder.and(comment.eventId.in(eventIds)));
        Optional.ofNullable(req.getUserId()).ifPresent(userId -> booleanBuilder.and(comment.authorId.eq(userId)));
        Optional.ofNullable(req.getRangeStart()).ifPresent(start -> booleanBuilder.and(comment.created.goe(start)));
        Optional.ofNullable(req.getRangeEnd()).ifPresent(end -> booleanBuilder.and(comment.created.loe(end)));
        Optional.ofNullable(req.getStatusList()).ifPresent(statusList -> booleanBuilder.and(comment.status.in(statusList)));

        return Optional.ofNullable(booleanBuilder.getValue());
    }

    private void checkUserIsCommentAuthor(Long userId, Comment comment) {
        if (!Objects.equals(comment.getAuthorId(), userId)) {
            throw new ConditionsConflictException("Пользователь с id=" + userId + " не является автором комментария id=" + comment.getId());
        }
    }

    private UserShortDto getUserShortDto(Long userId) {
        return userClientInternal.getUserShortDtoById(userId);
    }

    private List<UserShortDto> getUserShortDto(Set<Long> userIds) {
        return userClientInternal.getUserShortDtos(userIds);
    }

}
