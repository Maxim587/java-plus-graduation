# ExploreWithMe

Приложение для обмена информацией об интересных событиях и поиска пользователей для участия в них.

## Компоненты
Приложение использует принципы микросервисной архитектуры и состоит из следующих сервисов:
- `event-service` - управление событиями и подборками событий
- `category-service` - управление категориями событий
- `comment-service` - реализует функционал создания и модерации комментариев пользователями для событий
- `request-service` - управление запросами пользователей на участие в событиях
- `user-servive` - управление пользователями приложения
- `stats-server` - сервис статистики для анализа запросов пользователей на поиск событий

Инфрастуктура приложения реализована с использованием следующих компонентов:
- `config-server` - сервис конфигурации на основе Spring Cloud Config
- `discovery-server` - сервис обнаружения микросервисов на основе Netflix Eureka
- `gateway-server` - шлюз API на основе Spring Cloud Gateway

Взаимодействие между микросервисами производится через Feign-клиенты с использованием паттернов Retry и Circuit Breaker  на основе библиотеки Resilience4j. Feign-клиенты используют Apache HttpClient. Интерфейсы клиентов находятся в модуле `core/interaction-api/src/main/java/ru/practicum/feign`
Конфигурации микросервисов находятся в модуле `infra/config-server/src/main/resources/config`
Каждый микросервис использует собственную базу данных на основе СУБД PostgeSQL. Базы разворачиваются в контейнерах Docker.

## API
Приложение использует порт 8080
Спецификации OpenApi для внешних сервисов:
- основные сервисы [ewm-main-service-spec.json](https://github.com/Maxim587/java-plus-graduation#:~:text=ewm%2Dmain%2Dservice%2Dspec.json)
- сервис статистики [ewm-stats-service-spec.json](https://github.com/Maxim587/java-plus-graduation#:~:text=ewm%2Dstats%2Dservice%2Dspec.json)


Для взаимодействия между микросервисами используется внутренний API. Для всех методов используется базовый путь `/internal`

| Сервис | Эндпоинт                            | Описание                                                                                                                              |
| :--- |:------------------------------------|:--------------------------------------------------------------------------------------------------------------------------------------|
| category-service | `GET /categories/{categoryId}`      | Поиск категории  по Id                                                                                                                |
| category-service | `GET /categories`                   | Получение словаря categoryId:CategoryDto по списку Id категорий. Список передается в теле запроса                                     |
| comment-service | `GET /comments/map`                 | Получение словаря eventId:CommentsDto по списку Id событий. Список передается в теле запроса                                          |
| comment-service | `GET /comments/exists`              | Проверка существования комментария по Id автора. Параметры запроса: authorId                                                          |
| event-service | `GET /events/{eventId}`             | Получение события по Id                                                                                                               |
| event-service | `GET /events`                       | Получение события по Id категории или Id инициатора события. Параметры запроса: categoryId, initiatorId                               |
| request-service | `GET /request/{eventId}/confirmed`  | Получение количества подтвержденных заявок на событие для события                                                                     |
| request-service | `GET /request/confirmed`            | Получение словаря с количеством подтвержденных запросов для каждого события по списку Id событий. Список Id передается в теле запроса |
| request-service | `GET /request/exists`               | Проверка существования запроса по Id заявителя. Параметры запроса: requesterId                                                        |
| user-service | `GET /users/{userId}`               | Получение пользователя по Id                                                                                                          |
| user-service | `GET /users`                        | Получение списка пользователя по списку Id. Список Id пеедается в теле запроса                                                        |
| user-service | `GET /users/map`                    | Получение словаря userId:UserShortDto по списку Id пользователей. Список Id передается в теле запроса                                 |

