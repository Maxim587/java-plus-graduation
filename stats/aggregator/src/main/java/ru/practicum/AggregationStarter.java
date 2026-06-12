package ru.practicum;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.AggregatorKafkaConfig;
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
    private final KafkaConsumer<String, UserActionAvro> consumer;
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
                ConsumerRecords<String, UserActionAvro> records = consumer.poll(consumeAttemptTimeout);
                int count = 0;
                for (ConsumerRecord<String, UserActionAvro> record : records) {
                    userActionService.calculateSimilarity(record.value());
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
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, UserActionAvro> record, int count, KafkaConsumer<String, UserActionAvro> consumer) {
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

