package ru.practicum.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;
import java.util.Properties;


@Getter
@AllArgsConstructor
@ConfigurationProperties("kafka")
public class AnalyzerKafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private List<String> bootstrapServers;
    @Value("${kafka.consumer.properties.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${kafka.consumer.properties.enable-auto-commit}")
    private String enableAutoCommit;
    @Value("${kafka.consumer.properties.consume-attempt-timeout-ms}")
    private Long consumeAttemptTimeoutMs;

    //user action
    @Value("${kafka.consumer.properties.user-action.topics}")
    private String userActionsTopic;
    @Value("${kafka.consumer.properties.user-action.key.deserializer}")
    private String userActionKeyDeserializer;
    @Value("${kafka.consumer.properties.user-action.value.deserializer}")
    private String userActionValueDeserializer;
    @Value("${kafka.consumer.properties.user-action.group.id}")
    private String userActionsGroupId;

    //event similarity
    @Value("${kafka.consumer.properties.event-similarity.topics}")
    private String eventSimilarityTopic;
    @Value("${kafka.consumer.properties.event-similarity.key.deserializer}")
    private String eventSimilarityKeyDeserializer;
    @Value("${kafka.consumer.properties.event-similarity.value.deserializer}")
    private String eventSimilarityValueDeserializer;
    @Value("${kafka.consumer.properties.event-similarity.group.id}")
    private String eventSimilarityGroupId;


    @Bean
    public KafkaConsumer<String, UserActionAvro> userActionKafkaConsumer() {
        Properties properties = getUserActionKafkaConsumerConfig();
        return new KafkaConsumer<>(properties);
    }

    @Bean
    public KafkaConsumer<String, EventSimilarityAvro> eventSimilaritytKafkaConsumer() {
        Properties properties = getEventSimilarityKafkaConsumerConfig();
        return new KafkaConsumer<>(properties);
    }

    private Properties getBaseConfig() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return properties;
    }

    private Properties getUserActionKafkaConsumerConfig() {
        Properties properties = getBaseConfig();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, userActionKeyDeserializer);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, userActionValueDeserializer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, userActionsGroupId);
        return properties;
    }

    private Properties getEventSimilarityKafkaConsumerConfig() {
        Properties properties = getBaseConfig();
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, eventSimilarityKeyDeserializer);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, eventSimilarityValueDeserializer);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, eventSimilarityGroupId);
        return properties;
    }
}
