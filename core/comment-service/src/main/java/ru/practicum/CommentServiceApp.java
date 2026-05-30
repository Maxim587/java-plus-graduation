package ru.practicum;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients
@SpringBootApplication
@ConfigurationPropertiesScan
public class CommentServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApp.class, args);
    }
}