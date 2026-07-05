package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repositoty.EventSimilarityRepository;
import ru.practicum.repositoty.UserActionRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {
    private static final int MAX_NEIGHBOUR_EVENTS_COUNT = 5;
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository eventSimilarityRepository;

    @Override
    public Stream<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        long eventId = request.getEventId();
        int limit = request.getMaxResult();
        log.info("Обработка запроса на получение похожих событий для eventId={}", eventId);
        List<Long> userEventIds = userActionRepository.findEventIdsByUserIdAndEventIdIsNot(request.getUserId(), eventId);
        List<EventSimilarity> similarityList = eventSimilarityRepository.findAllByEventAOrEventB(eventId, eventId);
        return similarityList.stream()
                .filter(sim -> !userEventIds.contains(sim.getEventA()) && !userEventIds.contains(sim.getEventB()))
                .sorted(Comparator.comparing(EventSimilarity::getScore).reversed())
                .limit(limit)
                .map(sim -> getRecommendedEvent(sim, eventId));
    }

    @Override
    public Stream<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int limit = request.getMaxResult();
        log.info("Обработка запроса на получение рекомендаций для userId={}", userId);

        //Недавно просмотренные мероприятия пользователя
        Pageable eventsPage = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));
        List<Long> recentUserEventIds = userActionRepository.findAllByUserId(userId, eventsPage);
        log.info("Недавно просмотренные события пользователя {}", recentUserEventIds);

        if (recentUserEventIds.isEmpty()) {
            log.info("Событий нет, возвращаем пустой ответ");
            return Stream.empty();
        }

        //Список всех мероприятий, с которыми взаимодействовал пользователь
        List<UserAction> allUserEvents = userActionRepository.findAllByUserId(userId);
        Map<Long, UserAction> allUserEventsMap = allUserEvents.stream().collect(Collectors.toMap(UserAction::getEventId, Function.identity()));
        Set<Long> allUserEventIds = allUserEventsMap.keySet();
        log.info("Список всех мероприятий, с которыми взаимодействовал пользователь {}", allUserEventIds);

        //Мероприятия, похожие на recentUserEventIds, но с которыми пользователь не взаимодействовал
        Pageable similarEventsPage = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "score"));
        Set<Long> similarEventIdsUserNotInteractedWith = eventSimilarityRepository
                .findSimilarEvents(recentUserEventIds, allUserEventIds, similarEventsPage).stream()
                .map(sim -> allUserEventIds.contains(sim.getEventA()) ? sim.getEventB() : sim.getEventA())
                .collect(Collectors.toSet());
        log.info("Мероприятия, похожие на recentUserEventIds, но с которыми пользователь не взаимодействовал {}", similarEventIdsUserNotInteractedWith);

        //K просмотренных мероприятий, максимально похожих на предсказываемое, с которыми пользователь уже взаимодействовал.
        Pageable neighboursPage = PageRequest.of(0, MAX_NEIGHBOUR_EVENTS_COUNT, Sort.by(Sort.Direction.DESC, "score"));
        List<EventSimilarity> neighbourEvents = eventSimilarityRepository
                .findNeighbours(similarEventIdsUserNotInteractedWith, allUserEventIds, neighboursPage);

        Map<Long, Map<Long, Double>> newEventIdToNeighbourEvents = getNeighboursMap(similarEventIdsUserNotInteractedWith, neighbourEvents);
        log.info("Словарь новых событий, для каждого из которых подобрано {} максимально схожих просмотренных {}", MAX_NEIGHBOUR_EVENTS_COUNT, newEventIdToNeighbourEvents);

        //Сумма взвешенных оценок по каждому новому мероприятию
        Map<Long, Double> newEventIdToWeightedScoresSum = newEventIdToNeighbourEvents.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .map(entry1 -> allUserEventsMap.get(entry1.getKey()).getWeight() * entry1.getValue())
                                .reduce(0.0, Double::sum)
                ));
        log.info("Сумма взвешенных оценок по каждому новому мероприятию {}", newEventIdToWeightedScoresSum);

        //Сумма коэффициентов подобия по каждому новому мероприятию
        Map<Long, Double> similarityFactorsSum = newEventIdToNeighbourEvents.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> entry.getValue().values().stream()
                                .reduce(0.0, Double::sum)));
        log.info("Сумма коэффициентов подобия по каждому новому мероприятию {}", similarityFactorsSum);

        log.info("Завершена обработка запроса на получение рекомендаций для userId={}", userId);
        return newEventIdToWeightedScoresSum.entrySet().stream()
                .map(e -> getRecommendedEventProto(e.getKey(), e.getValue() / similarityFactorsSum.get(e.getKey())));
    }

    @Override
    public Stream<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        log.info("Обработка запроса на получение рейтинга для событий={}", request.getEventIdList());
        return userActionRepository.findAllByEventIdIn(request.getEventIdList()).stream()
                .collect(Collectors.groupingBy(UserAction::getEventId, Collectors.summarizingDouble(UserAction::getWeight)))
                .entrySet().stream()
                .map(e -> getRecommendedEventProto(e.getKey(), e.getValue().getSum()));
    }

    private RecommendedEventProto getRecommendedEvent(EventSimilarity eventSimilarity, long eventId) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventSimilarity.getEventA() == eventId ? eventSimilarity.getEventB() : eventSimilarity.getEventA())
                .setScore(eventSimilarity.getScore())
                .build();
    }

    private Map<Long, Map<Long, Double>> getNeighboursMap(Set<Long> notInteractedEventIds, List<EventSimilarity> neighbourEvents) {
        Map<Long, Map<Long, Double>> neighboursMap = new HashMap<>();
        for (EventSimilarity event : neighbourEvents) {
            long notInteractedEventId;
            long interactedEventId;
            if (notInteractedEventIds.contains(event.getEventA())) {
                notInteractedEventId = event.getEventA();
                interactedEventId = event.getEventB();
            } else {
                notInteractedEventId = event.getEventB();
                interactedEventId = event.getEventA();
            }
            neighboursMap.computeIfAbsent(notInteractedEventId, k -> new HashMap<>())
                    .put(interactedEventId, event.getScore());
        }
        return neighboursMap;
    }

    private RecommendedEventProto getRecommendedEventProto(long eventId, double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}
