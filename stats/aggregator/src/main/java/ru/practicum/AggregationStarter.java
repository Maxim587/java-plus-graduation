package ru.practicum;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.AggregatorKafkaConfig;
import ru.practicum.kafka.AggregatorKafkaProducer;
import ru.practicum.service.UserActionService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private static final int MIN_RECORDS_AMOUNT_TO_COMMIT_OFFSETS = 10;
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final AggregatorKafkaProducer aggregatorKafkaProducer;
    private final AggregatorKafkaConfig config;
    private final UserActionService userActionService;

    public void start() {
        log.info("Запуск сервиса aggregator");
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        Duration consumeAttemptTimeout = Duration.ofMillis(config.getConsumeAttemptTimeoutMs());
        List<String> topics = List.of(config.getUserActionsTopic());
        try {
            log.info("Создание подписки на топики: {}", topics);
            consumer.subscribe(topics);
            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(consumeAttemptTimeout);
                int count = 0;
                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.info("Начало обработки записи из топика о действии пользователя {}", record);
                    handleRecord(record);
                    log.info("Завершена обработка записи из топика о действии пользователя {}", record);
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
        } catch (Exception e) {
            log.info("Ошибка во время обработки действий пользователя", e);
        } finally {
            try {
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Завершение работы консьюмера");
                consumer.close();
                log.info("Завершение работы продюсера");
                aggregatorKafkaProducer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<Long, UserActionAvro> record, int count, KafkaConsumer<Long, UserActionAvro> consumer) {
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );
        if (count % MIN_RECORDS_AMOUNT_TO_COMMIT_OFFSETS == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private void handleRecord(ConsumerRecord<Long, UserActionAvro> record) {
        List<EventSimilarityAvro> similarities = userActionService.calculateSimilarity(record.value());

        if (similarities.isEmpty()) {
            log.info("Список схожих событий пустой");
            return;
        }

        log.info("Начало отправки записей в количестве {} в топик {}", similarities.size(), config.getEventsSimilarityTopic());
        for (EventSimilarityAvro similarity : similarities) {
            long timestamp = similarity.getTimestamp().toEpochMilli();
            ProducerRecord<Long, SpecificRecordBase> similarityRecord = new ProducerRecord<>(
                    config.getEventsSimilarityTopic(),
                    null,
                    timestamp,
                    similarity.getEventA(),
                    similarity
            );

            aggregatorKafkaProducer.send(similarityRecord);
        }
        log.info("Завершена отправка записей в количестве {} в топик {}", similarities.size(), config.getEventsSimilarityTopic());
    }
}

