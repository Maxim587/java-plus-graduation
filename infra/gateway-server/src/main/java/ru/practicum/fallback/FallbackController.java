package ru.practicum.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback")
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomFeignError sendFallback() {
        log.error("ApiGateway fallback: service unavailable");
        return new CustomFeignError(
                "Сервис временно недоступен",
                "Ошибка сервера",
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
