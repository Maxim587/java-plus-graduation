package ru.practicum.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.List;
import java.util.Properties;


@Getter
@AllArgsConstructor
@ConfigurationProperties("kafka")
public class AggregatorKafkaConfig {
    @Value("${kafka.bootstrap-servers}")
    private List<String> bootstrapServers;

    //  consumer
    @Value("${kafka.consumer.topics.user-actions}")
    private String userActionsTopic;
    @Value("${kafka.consumer.properties.key.deserializer}")
    private String keyDeserializer;
    @Value("${kafka.consumer.properties.value.deserializer}")
    private String valueDeserializer;
    @Value("${kafka.consumer.properties.group.id}")
    private String groupId;
    @Value("${kafka.consumer.properties.consume.attempt.timeout.ms}")
    private Long consumeAttemptTimeoutMs;
    @Value("${kafka.consumer.properties.auto-offset-reset}")
    private String autoOffsetReset;
    @Value("${kafka.consumer.properties.enable-auto-commit}")
    private String enableAutoCommit;

    //  producer
    @Value("${kafka.producer.topics.events-similarity}")
    private String eventsSimilarityTopic;
    @Value("${kafka.producer.properties.client.id}")
    private String clientId;
    @Value("${kafka.producer.properties.key.serializer}")
    private String keySerializer;
    @Value("${kafka.producer.properties.value.serializer}")
    private String valueSerializer;


    @Bean
    public KafkaConsumer<Long, UserActionAvro> kafkaConsumer() {
        Properties properties = getConsumerConfig();
        return new KafkaConsumer<>(properties);
    }

    @Bean
    public Producer<Long, SpecificRecordBase> producer() {
        return new KafkaProducer<>(getProducerConfig());
    }

    @Bean
    public AggregatorKafkaProducer aggregatorKafkaProducer(Producer<Long, SpecificRecordBase> producer) {
        return new AggregatorKafkaProducer(producer);
    }

    private Properties getConsumerConfig() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
//        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
//        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        return properties;
    }

    private Properties getProducerConfig() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//        config.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        return config;
    }
}
