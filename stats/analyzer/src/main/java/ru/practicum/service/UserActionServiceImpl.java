package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.UserActionMapper;
import ru.practicum.model.UserAction;
import ru.practicum.repositoty.UserActionRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {
    private final UserActionRepository repository;
    private final UserActionMapper mapper;

    @Override
    @Transactional
    public void saveUserAction(UserActionAvro userActionAvro) {
        log.info("Обработка запроса на сохранение действия пользователя: {}", userActionAvro);
        UserAction userActionNew = mapper.mapUserActionAvroToUserAction(userActionAvro);
        Optional<UserAction> userActionOpt = repository.findByUserIdAndEventId(userActionAvro.getUserId(), userActionAvro.getEventId());
        if (userActionOpt.isEmpty()) {
            repository.save(userActionNew);
        } else {
            UserAction userActionDb = userActionOpt.get();
            if (userActionNew.getWeight() > userActionDb.getWeight()) {
                userActionDb.setWeight(userActionNew.getWeight());
                repository.save(userActionDb);
            }
        }
        log.info("Завершена обработка запроса на сохранение действия пользователя: {}", userActionAvro);
    }
}
