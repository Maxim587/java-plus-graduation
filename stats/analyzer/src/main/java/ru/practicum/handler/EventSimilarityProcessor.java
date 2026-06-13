package ru.practicum.handler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.kafka.AnalyzerKafkaConfig;
import ru.practicum.service.EventSimilarityService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {
    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();
    private static final int MIN_RECORDS_AMOUNT_TO_COMMIT_OFFSETS = 10;
    private final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    private final EventSimilarityService similarityService;
    private final AnalyzerKafkaConfig config;


    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
        Duration consumeAttemptTimeout = Duration.ofMillis(config.getConsumeAttemptTimeoutMs());
        List<String> topics = List.of(config.getEventSimilarityTopic());
        try {
            log.debug("Создание подписки на топики: {}", topics);
            consumer.subscribe(topics);
            while (true) {
                int count = 0;
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(consumeAttemptTimeout);
                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    log.info("Получено сообщение {} из топика: {}", record.value(), record.topic());
                    log.info("Сообщение отправляется в обработчик");
                    similarityService.saveEventSimilarity(record.value());
                    manageOffsets(record, count, consumer);
                    count++;
                }
                consumer.commitAsync();
            }
        } catch (WakeupException ignores) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки сообщений сходства событий", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Завершение работы консьюмера");
                consumer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<Long, EventSimilarityAvro> record, int count, KafkaConsumer<Long, EventSimilarityAvro> consumer) {
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
}
