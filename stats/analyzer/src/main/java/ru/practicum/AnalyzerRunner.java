package ru.practicum;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.handler.EventSimilarityProcessor;
import ru.practicum.handler.UserActionProcessor;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    private final UserActionProcessor userActionProcessor;
    private final EventSimilarityProcessor eventSimilarityProcessor;

    @Override
    public void run(String... args) {
        Thread hubEventsThread = new Thread(eventSimilarityProcessor);
        hubEventsThread.setName("eventSimilarityProcessorThread");
        hubEventsThread.start();

        userActionProcessor.start();
    }
}
