package ru.practicum.mapper;

import com.google.protobuf.Timestamp;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserActionMapper {

    @ValueMapping(target = MappingConstants.THROW_EXCEPTION, source = "UNRECOGNIZED")
    UserActionAvro mapUserActionProtoToAvro(UserActionProto userActionProto);

    default Instant mapTimestampToInstant(Timestamp timestamp) {
        return timestamp == null ? null : Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    default ActionTypeAvro mapActionTypeProtoToAvro(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case UNRECOGNIZED -> throw new  IllegalArgumentException("Unrecognized action type");
        };
    }
}
