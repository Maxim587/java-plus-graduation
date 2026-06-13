package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.CollectorKafkaProducer;
import ru.practicum.kafka.KafkaProducerConfig;
import ru.practicum.mapper.UserActionMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandlerImpl implements UserActionHandler {
    private final UserActionMapper mapper;
    private final KafkaProducerConfig kafkaProducerConfig;
    private final CollectorKafkaProducer producer;

    @Override
    public void handle(UserActionProto userActionProto) {
        log.info("Обработка запроса на обработку действия пользователя event_id={}, user_id={}, action_type={}", userActionProto.getEventId(), userActionProto.getUserId(), userActionProto.getActionType());
        UserActionAvro avro = mapper.mapUserActionProtoToAvro(userActionProto);
        ProducerRecord<String, SpecificRecordBase> record =
                new ProducerRecord<>(kafkaProducerConfig.getUserActionsTopic(), avro);
        producer.send(record);
    }
}
