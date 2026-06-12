package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.CollectorKafkaProducer;
import ru.practicum.kafka.KafkaProducerConfig;
import ru.practicum.mapper.UserActionMapper;


@Component
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {
    private final UserActionMapper mapper;
    private final KafkaProducerConfig kafkaProducerConfig;
    private final CollectorKafkaProducer producer;

    @Override
    public void handle(UserActionProto userActionProto) {
        UserActionAvro avro = mapper.mapUserActionProtoToAvro(userActionProto);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(kafkaProducerConfig.getUserActionsTopic(), avro);
        producer.send(record);
    }
}
