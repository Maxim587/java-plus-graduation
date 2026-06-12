package ru.practicum.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface UserActionService {

    void calculateSimilarity(UserActionAvro userActionAvro);
}
