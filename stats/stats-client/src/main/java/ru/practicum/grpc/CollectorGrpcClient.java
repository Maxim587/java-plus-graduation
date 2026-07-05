package ru.practicum.grpc;


import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.enums.UserActionType;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Component
public class CollectorGrpcClient {
    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void collectUserAction(long userId, long eventId, UserActionType actionType) {
        Instant now = Instant.now();
        Timestamp ts = Timestamp.newBuilder()
                .setSeconds(now.getEpochSecond())
                .setNanos(now.getNano())
                .build();

        UserActionProto userActionProto = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(getActionTypeProto(actionType))
                .setTimestamp(ts)
                .build();

        client.collectUserAction(userActionProto);
    }

    private ActionTypeProto getActionTypeProto(UserActionType actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeProto.ACTION_VIEW;
            case ACTION_REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case ACTION_LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }
}
