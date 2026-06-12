package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.mapper.EventSimilarityMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repositoty.EventSimilarityRepository;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final EventSimilarityRepository repository;
    private final EventSimilarityMapper mapper;


    @Override
    @Transactional
    public void saveEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        EventSimilarity eventSimilarityNew = mapper.mapUserActionAvroToUserAction(eventSimilarityAvro);
        Optional<EventSimilarity> eventSimilarityOpt = repository.findByEventAAndEventB(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        if (eventSimilarityOpt.isEmpty()) {
            repository.save(eventSimilarityNew);
        } else {
            EventSimilarity eventSimilarityDb = eventSimilarityOpt.get();
            if (eventSimilarityDb.getScore() != eventSimilarityNew.getScore()) {
                eventSimilarityDb.setScore(eventSimilarityNew.getScore());
                repository.save(eventSimilarityDb);
            }
        }
    }
}
