package ru.practicum.controller;


import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc.UserActionControllerImplBase;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.handler.UserActionHandler;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserActionControllerGrpc extends UserActionControllerImplBase {
    private final UserActionHandler userActionHandler;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        log.info("Получен запрос на обработку действия пользователя event_id={}, user_id={}, action_type={}", request.getEventId(), request.getUserId(), request.getActionType());
        try {
            userActionHandler.handle(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
