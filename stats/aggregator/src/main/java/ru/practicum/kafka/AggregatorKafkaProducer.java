package ru.practicum.kafka;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Future;

@Getter
@Slf4j
@Component
public class AggregatorKafkaProducer implements AutoCloseable {
    private static final Duration PRODUCER_CLOSE_TIMEOUT = Duration.ofMillis(5L);
    private final Producer<String, SpecificRecordBase> producer;

    public AggregatorKafkaProducer(Producer<String, SpecificRecordBase> producer) {
        this.producer = producer;
    }

    public Future<RecordMetadata> send(ProducerRecord<String, SpecificRecordBase> record) {
        log.debug("Отправка записи в kafka: {}", record);
        Future<RecordMetadata> future = producer.send(record);
        producer.flush();
        return future;
    }

    @Override
    public void close() {
        log.info("Отправка данных из буфера продюсера перед закрытием");
        producer.flush();
        log.info("Закрытие продюсера с таймаутом {} мс", PRODUCER_CLOSE_TIMEOUT.toMillis());
        producer.close(PRODUCER_CLOSE_TIMEOUT);
    }
}
