package ru.practicum.feign.config;


import feign.Feign;
import feign.hc5.ApacheHttp5Client;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.feign.config.errordecoder.CustomErrorDecoder;

@Configuration
public class FeignCustomConfig {

    @Bean
    public Feign.Builder feignBuilder() {
        return Feign.builder()
                .errorDecoder(new CustomErrorDecoder())
                .client(new ApacheHttp5Client(HttpClients.custom().build()));
    }
}
