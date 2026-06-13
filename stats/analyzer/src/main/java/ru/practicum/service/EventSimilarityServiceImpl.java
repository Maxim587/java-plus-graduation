package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.mapper.EventSimilarityMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repositoty.EventSimilarityRepository;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {
    private final EventSimilarityRepository repository;
    private final EventSimilarityMapper mapper;


    @Override
    @Transactional
    public void saveEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Обработка запроса на сохранение коэффициента сходства в БД {}", eventSimilarityAvro);
        EventSimilarity eventSimilarityNew = mapper.mapUserActionAvroToUserAction(eventSimilarityAvro);
        Optional<EventSimilarity> eventSimilarityOpt = repository.findByEventAAndEventB(eventSimilarityAvro.getEventA(), eventSimilarityAvro.getEventB());
        if (eventSimilarityOpt.isEmpty()) {
            log.info("Для переданных событий коэффициент в БД не найден. Сохраняем новый");
            repository.save(eventSimilarityNew);
        } else {
            log.info("Для переданных событий коэффициент в БД найден");
            EventSimilarity eventSimilarityDb = eventSimilarityOpt.get();
            if (eventSimilarityDb.getScore() != eventSimilarityNew.getScore()) {
                eventSimilarityDb.setScore(eventSimilarityNew.getScore());
                log.info("Переданный и новый коэффициент отличаются. Обновляем в БД");
                repository.save(eventSimilarityDb);
            }
        }
        log.info("Завершена обработка запроса на сохранение коэффициента сходства в БД {}", eventSimilarityAvro);
    }
}
