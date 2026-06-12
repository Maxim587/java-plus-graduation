package ru.practicum.handler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.kafka.AnalyzerKafkaConfig;
import ru.practicum.service.EventSimilarityService;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityProcessor implements Runnable {
    private final KafkaConsumer<String, EventSimilarityAvro> consumer;
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
                ConsumerRecords<String, EventSimilarityAvro> records = consumer.poll(consumeAttemptTimeout);
                for (ConsumerRecord<String, EventSimilarityAvro> record : records) {
                    log.info("Получено сообщение {} из топика: {}", record.value(), record.topic());
                    log.info("Сообщение отправляется в обработчик");
                    similarityService.saveEventSimilarity(record.value());
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
}
