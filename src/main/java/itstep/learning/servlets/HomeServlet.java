package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.config.ConfigService;
import itstep.learning.services.hash.HashService;
import itstep.learning.services.hash.Md5HashService;
import itstep.learning.services.kdf.KdfService;
import itstep.learning.services.random.DateTimeService;
import itstep.learning.services.random.RandomService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gson.Gson;


import java.util.logging.Level;
import java.util.logging.Logger;



@Singleton
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(HomeServlet.class.getName());

    private final RandomService randomService;
    private final DateTimeService dateTimeService;
    private final KdfService kdfService;
    private final DataContext dataContext;
    private final ConfigService configService;

    @Inject
    public HomeServlet(RandomService randomService,
                       DateTimeService dateTimeService,
                       KdfService kdfService,
                       DataContext dataContext,
                       ConfigService configService) {
        this.randomService = randomService;
        this.dateTimeService = dateTimeService;
        this.kdfService = kdfService;
        this.dataContext = dataContext;
        this.configService = configService;
    }

    public HomeServlet() {
        this.randomService = null;
        this.dateTimeService = null;
        this.kdfService = null;
        this.dataContext = null;
        this.configService = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCorsHeaders(resp);

        Map<String, Object> response = new HashMap<>();
        int statusCode = 200;

        try {
            if (randomService == null || dateTimeService == null || kdfService == null
                    || dataContext == null || configService == null) {
                throw new IllegalStateException("Не удалось загрузить все зависимости через Guice.");
            }

            // Инициализация БД (создание таблиц, базовых ролей и т.п.)
            dataContext.initializeRolesAndAccess();

            // Получаем параметры из конфигурации
            int lifetime = configService.getInt("jwt.lifetime");      // пример: 100
            String db = configService.getString("db.MySql.dbms");         // пример: "MySql"
            String host = configService.getString("db.MySql.host");       // пример: "localhost"

            // Пример получения "someConfigKey" (если нужно)
            // String someConfig = configService.getString("someConfigKey");

            // Генерация случайного числа
            int randomNumber = randomService.randomInt();

            // Генерация случайной строки (длина 9 символов, как в примере)
            String randomString = randomService.randomString(9);

            // Генерация случайного имени файла
            String randomFileName = randomService.randomFileName(12);

            // Хэширование сообщения
            String hashedMessage = kdfService.dk("123", "456");

            // Создание таблиц через DataContext
            boolean tablesCreated = dataContext.installTables();
            String tablesMessage = tablesCreated
                    ? "install ok"
                    : "Ошибка при создании таблиц. Проверьте логи для деталей.";

            // Получение данных через UserDao
            String currentTime = dataContext.getUserDao().fetchCurrentTime();
            String databases = dataContext.getUserDao().fetchDatabases();

            // Формируем итоговый JSON-ответ
            response.put("tablesMessage", tablesMessage);                       // "install ok"
            response.put("currentTime", currentTime != null ? currentTime : "");
            response.put("databases", databases != null ? databases : "");      // "information_schema, java221, performance_schema"
            response.put("randomNumber", randomNumber);                         // "1656679532"
            response.put("randomString", randomString);                         // "7lVJgZItc8"
            response.put("randomFileName", randomFileName);                     // "a7739a249fea.txt"
            response.put("hashedMessage", hashedMessage);                       // "85ec1a9b766c9a39d42bd10cfa7fb66f2bd45e6563320d2a9fdc63fd0a5cd1f0"
            response.put("message", "Запит виконано успішно.");
            response.put("lifetime", lifetime);                                 // "100"
            response.put("db", db);                                             // "MySql"
            response.put("host", host);                                         // "localhost"
            response.put("status", statusCode);
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Ошибка загрузки зависимостей", e);
            response.put("message", "Ошибка загрузки зависимостей: " + e.getMessage());
            statusCode = 500;
            response.put("status", statusCode);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Неожиданная ошибка", e);
            response.put("message", "Виникла несподівана помилка: " + e.getMessage());
            statusCode = 500;
            response.put("status", statusCode);
        }

        resp.setStatus(statusCode);
        resp.getWriter().print(new Gson().toJson(response));
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
/**
 * IoC (Inversion of Control) — Інверсія управління
 * Архітектурний патерн, за яким управління (життєвим циклом об'єктів)
 * передається спеціалізованому модулю (контейнер служб, інжектор, Resolver).
 *
 * Основні етапи:
 *
 * 1. Реєстрація:
 *    Додавання інформації до контейнера, зазвичай у формі:
 *    [тип - час життя (scope)]
 *    Приклад:
 *    - Тип 1
 *    - Тип 2
 *    - Тип 3
 *
 *    Контейнер відповідає за управління цими об'єктами.
 *
 * 2. Resolve:
 *    Клас -> (через контейнер) -> Об'єкт (у тому числі вже існуючий, а не новий).
 *    Наприклад:
 *      Connection
 *
 * Приклад коду для впровадження залежностей:
 *
 * class SomeService {
 *     private final Connection _conn;
 *     private final Logger _logger;
 *
 *     public SomeService(Connection c, Logger logger) {
 *         _conn = c;
 *         _logger = logger;
 *     }
 * }
 *


 Асинхронне виконання коду

 1. Синхронне виконання – послідовне виконання операцій у часі:
 ++++++++++------------
 ********************

 2. Асинхронність – будь-яке відхилення від синхронного виконання:
 ++++++++
 --------  // Паралельне виконання
 ********

 ++++++++--------
 ********  // Ниткове виконання

 +   +
 -   -  // Паралельне з перемиканням
 *   *

 -----------------------------------------------
 Способи реалізації асинхронності:
 - **Багатозадачність** – виконання кількох завдань одночасно (Task, Future).
 - **Багатопотоковість** – використання кількох потоків в межах одного процесу.
 - **Багатопроцесність** – виконання декількох процесів одночасно.
 - **Мережеві технології** – асинхронний обмін даними (Grid, Network).

 -----------------------------------------------
 Асинхронність у різних мовах:

 JavaScript (ES6+):
 async function fun() { ... }

 let res = await fun();  // X

 let task = fun();  // Запускаємо без очікування
 інший код...       // Виконується інший код
 res = await task;  // !

 Використання `then()` з `await`:
 res = await fun().then(...).then(...);  // ?

 fun().then(...).then(...).then(res => ... );  // !

 -----------------------------------------------
 Основні проблеми при роботі з асинхронністю:
 - **Callback Hell** – надмірне вкладення викликів, ускладнює читання коду.
 - **Race Condition** – конфлікти при одночасному доступі до спільних ресурсів.
 - **Deadlock** – стан, коли процеси чекають одне одного і не можуть продовжити роботу.
 - **Starvation** – ситуація, коли один процес не отримує ресурсів через інші завдання.

 -----------------------------------------------
 Приклад асинхронного запиту до API:

 async function fetchData() {
     try {
         let response = await fetch("https://api.example.com/data");
         let data = await response.json();
         console.log("Отримані дані:", data);
     } catch (error) {
         console.error("Помилка запиту:", error);
     }
 }
 fetchData();

 -----------------------------------------------
 Коли використовувати асинхронне програмування:
 - Робота з базами даних (запити до сервера).
 - Виконання тривалих операцій без блокування інтерфейсу.
 - Обробка великого обсягу даних у фоновому режимі.
 - Паралельні обчислення для покращення продуктивності.
*/