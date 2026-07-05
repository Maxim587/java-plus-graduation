package ru.practicum.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static ru.practicum.constant.StatsConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final Map<Long, Map<Long, Double>> eventUserActionsMaxWeightMatrix = new HashMap<>();   //Map<Event, Map<User, MaxWeight>>
    private final Map<Long, Map<Long, Double>> minWeightsSum = new HashMap<>();                     //Map<Event1, Map<Event2, S_min>>
    private final Map<Long, Double> eventWeightsSum = new HashMap<>();                              //Map<Event, UserActionWeightsSum>


    public List<EventSimilarityAvro> calculateSimilarity(UserActionAvro userActionAvro) {
        log.info("Обработка запроса на расчет сходства событий {}", userActionAvro);
        long userId = userActionAvro.getUserId();
        long eventIdA = userActionAvro.getEventId();
        double newWeightA = getActionWeight(userActionAvro.getActionType());

        Map<Long, Double> userActionWeightMap = eventUserActionsMaxWeightMatrix.get(eventIdA);
        log.info("Получен словарь максимальных весов действий пользователей для события eventId={}. Словарь: {}", eventIdA, userActionWeightMap);
        double oldWeightA;

        if (userActionWeightMap == null) {
            log.info("Пользователь userId={} ранее не взаимодействовал с событием eventId={}", userId, eventIdA);
            oldWeightA = 0.0;
            userActionWeightMap = new HashMap<>();
            userActionWeightMap.put(userId, newWeightA);
            eventUserActionsMaxWeightMatrix.put(eventIdA, userActionWeightMap);
            log.info("Обновляем матрицу весов. Новая матрица = {}", eventUserActionsMaxWeightMatrix);
        } else {
            log.info("Получаем текущий вес действия пользователя userId={}", userId);
            oldWeightA = userActionWeightMap.getOrDefault(userId, 0.0);
            log.info("Текущий вес действия = {}", oldWeightA);
            if (newWeightA > oldWeightA) {
                userActionWeightMap.put(userId, newWeightA);
                log.info("Новый вес действия {} больше текущего {}. Обновляем матрицу весов. Новая матрица = {}", newWeightA, oldWeightA, eventUserActionsMaxWeightMatrix);
            } else {
                log.info("Новый вес действия {} меньше или равен текущему {}. Завершаем расчет", newWeightA, oldWeightA);
                return Collections.emptyList();
            }
        }

        log.info("Обновляем словарь сумм весов действий для eventId={}. Разница весов = {}-{}={}. Текущий словарь = {}", eventIdA, newWeightA, oldWeightA, newWeightA - oldWeightA, eventWeightsSum);
        eventWeightsSum.compute(eventIdA, (event, sum) -> (sum == null ? 0.0 : sum) + newWeightA - oldWeightA);
        log.info("Новый словарь = {}", eventWeightsSum);

        Map<Long, Map<Long, Double>> otherEventsUserInteracted = eventUserActionsMaxWeightMatrix.entrySet().stream()
                .filter(e -> e.getValue().containsKey(userId) && !e.getKey().equals(eventIdA))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        log.info("Получаем матрицу весов других событий, с которыми пользователь взаимодействовал = {}", otherEventsUserInteracted);

        log.info("Формируем суммы минимальных весов для пар событий и рассчитываем коэффициенты сходства");
        List<EventSimilarityAvro> similarityAvros = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : otherEventsUserInteracted.entrySet()) {
            Long eventIdB = entry.getKey();
            double weightB = entry.getValue().get(userId);
            double minWeightABOld = Math.min(oldWeightA, weightB);
            double minWeightABNew = Math.min(newWeightA, weightB);
            double weightsDifference = minWeightABNew - minWeightABOld;
            double sMin = getSum(eventIdA, eventIdB);
            log.info("Текущая сумма минимальных весов для событий {} и {} = {}", eventIdA, eventIdB, sMin);
            if (weightsDifference > 0) {
                sMin = sMin + weightsDifference;
                putSum(eventIdA, eventIdB, sMin);
                log.info("Обновлена сумма минимальных весов для событий {} и {} на разницу {}. Обновленная сумма = {}", eventIdA, eventIdB, weightsDifference, sMin);
            }

            double weightSumA = eventWeightsSum.get(eventIdA);
            double weightSumB = eventWeightsSum.get(eventIdB);
            double similarityScore = sMin / (Math.sqrt(weightSumA) * Math.sqrt(weightSumB));
            log.info("Рассчитан коэффициент сходства между событиями {} и {}. Коэффициент = {}", eventIdA, eventIdB, similarityScore);

            EventSimilarityAvro eventSimilarityAvro = getEventSimilarityAvro(eventIdA, eventIdB, similarityScore);
            similarityAvros.add(eventSimilarityAvro);
        }
        log.info("Сформирован список схожих событий {}", similarityAvros);
        log.info("Завершена обработка запроса на расчет сходства событий {}", userActionAvro);
        return similarityAvros;
    }

    private EventSimilarityAvro getEventSimilarityAvro(long eventIdA, long eventIdB, double similarityScore) {
        long firstEvent = Math.min(eventIdA, eventIdB);
        long secondEvent = Math.max(eventIdA, eventIdB);
        return EventSimilarityAvro.newBuilder()
                .setEventA(firstEvent)
                .setEventB(secondEvent)
                .setScore(similarityScore)
                .setTimestamp(Instant.now())
                .build();
    }

    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> ACTION_VIEW_WEIGHT;
            case REGISTER -> ACTION_REGISTER_WEIGHT;
            case LIKE -> ACTION_LIKE_WEIGHT;
        };
    }

    private void putSum(long eventA, long eventB, double sum) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        minWeightsSum
                .computeIfAbsent(first, e -> new HashMap<>())
                .put(second, sum);
    }

    private double getSum(long eventA, long eventB) {
        long first = Math.min(eventA, eventB);
        long second = Math.max(eventA, eventB);

        return minWeightsSum
                .computeIfAbsent(first, e -> new HashMap<>())
                .getOrDefault(second, 0.0);
    }
}
