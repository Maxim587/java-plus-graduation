package ru.practicum.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.AggregatorKafkaConfig;
import ru.practicum.kafka.AggregatorKafkaProducer;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.practicum.constant.StatsConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final AggregatorKafkaProducer producer;
    private final AggregatorKafkaConfig kafkaConfig;
    private final Map<Long, Map<Long, Double>> eventUserActionsMaxWeightMatrix = new HashMap<>();   //Map<Event, Map<User, MaxWeight>>
    private final Map<Long, Map<Long, Double>> minWeightsSum = new HashMap<>();                     //Map<Event1, Map<Event2, S_min>>
    private final Map<Long, Double> eventWeightsSum = new HashMap<>();                              //Map<Event, UserActionWeightsSum>


    public void calculateSimilarity(UserActionAvro userActionAvro) {
        log.info("Начало расчета сходства событий {}", userActionAvro);
        long userId = userActionAvro.getUserId();
        long eventIdA = userActionAvro.getEventId();
        double newWeightA = getActionWeight(userActionAvro.getActionType());

        Map<Long, Double> userActionWeightMap = eventUserActionsMaxWeightMatrix.get(eventIdA);
        double oldWeightA;

        if (userActionWeightMap == null) {
            oldWeightA = 0.0;
            userActionWeightMap = new HashMap<>();
            userActionWeightMap.put(userId, newWeightA);
            eventUserActionsMaxWeightMatrix.put(eventIdA, userActionWeightMap);
        } else {
            oldWeightA = userActionWeightMap.get(userId);
            if (newWeightA > oldWeightA) {
                userActionWeightMap.put(userId, newWeightA);
            } else {
                return;
            }
        }

        eventWeightsSum.compute(eventIdA, (event, sum) -> (sum == null ? 0.0 : sum) + newWeightA - oldWeightA);

        Map<Long, Map<Long, Double>> otherEventsUserInteracted = eventUserActionsMaxWeightMatrix.entrySet().stream()
                .filter(e -> e.getValue().containsKey(userId) && !e.getKey().equals(eventIdA))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<Long, Map<Long, Double>> entry : otherEventsUserInteracted.entrySet()) {
            Long eventIdB = entry.getKey();
            double weightB = entry.getValue().get(userId);
            double minWeightABOld = Math.min(oldWeightA, weightB);
            double minWeightABNew = Math.min(newWeightA, weightB);
            double weightsDifference = minWeightABNew - minWeightABOld;
            double sMin = getSum(eventIdA, eventIdB);
            if (weightsDifference > 0) {
                sMin = sMin + weightsDifference;
                putSum(eventIdA, eventIdB, sMin);
            }

            double weightSumA = eventWeightsSum.get(eventIdA);
            double weightSumB = eventWeightsSum.get(eventIdB);
            double similarityScore = sMin / (Math.sqrt(weightSumA) * Math.sqrt(weightSumB));

            EventSimilarityAvro eventSimilarityAvro = new EventSimilarityAvro(eventIdA, eventIdB, similarityScore, Instant.now());
            producer.send(new ProducerRecord<>(kafkaConfig.getEventsSimilarityTopic(), eventSimilarityAvro));
        }
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
