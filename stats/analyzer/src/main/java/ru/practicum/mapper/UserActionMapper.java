package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.UserAction;

import static ru.practicum.constant.StatsConstants.*;


@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserActionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "weight", expression = "java(getActionWeight(userActionAvro.getActionType()))")
    UserAction mapUserActionAvroToUserAction(UserActionAvro userActionAvro);


    default double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> ACTION_VIEW_WEIGHT;
            case REGISTER -> ACTION_REGISTER_WEIGHT;
            case LIKE -> ACTION_LIKE_WEIGHT;
        };
    }
}
