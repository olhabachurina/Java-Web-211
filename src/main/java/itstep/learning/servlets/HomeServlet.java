package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.services.DbService.DbService;
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

    @Inject
    public HomeServlet(RandomService randomService, DateTimeService dateTimeService, KdfService kdfService, DataContext dataContext) {
        this.randomService = randomService;
        this.dateTimeService = dateTimeService;
        this.kdfService = kdfService;
        this.dataContext = dataContext;
    }

    public HomeServlet() {
        this.randomService = null;
        this.dateTimeService = null;
        this.kdfService = null;
        this.dataContext = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCorsHeaders(resp);

        Map<String, Object> response = new HashMap<>();
        int statusCode = 200;

        try {
            if (randomService == null || dateTimeService == null || kdfService == null || dataContext == null) {
                throw new IllegalStateException("Не удалось загрузить все зависимости через Guice.");
            }

            // Генерация случайного числа
            int randomNumber = randomService.randomInt();

            // Генерация случайной строки
            String randomString = randomService.randomString(10);

            // Генерация случайного имени файла
            String randomFileName = randomService.randomFileName(12);

            // Хэширование сообщения
            String hashedMessage = kdfService.dk("123", "456");

            // Создание таблиц через DataContext
            boolean tablesCreated = dataContext.installTables();
            String tablesMessage = tablesCreated
                    ? "install ok"
                    : "Ошибка при создании таблиц. Проверьте логи для деталей.";
            response.put("tablesMessage", tablesMessage);

            // Получение данных через UserDao из DataContext
            String currentTime = dataContext.getUserDao().fetchCurrentTime();
            String databases = dataContext.getUserDao().fetchDatabases();

            // Формирование успешного ответа
            response.put("currentTime", currentTime != null ? currentTime : "Не удалось получить текущее время");
            response.put("databases", databases != null ? databases : "Не удалось получить список баз данных");
            response.put("randomNumber", randomNumber);
            response.put("randomString", randomString);
            response.put("randomFileName", randomFileName);
            response.put("hashedMessage", hashedMessage);
            response.put("message", "Запит виконано успішно.");

        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Ошибка загрузки зависимостей", e);
            response.put("message", "Ошибка загрузки зависимостей: " + e.getMessage());
            statusCode = 500;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Неожиданная ошибка", e);
            response.put("message", "Виникла несподівана помилка: " + e.getMessage());
            statusCode = 500;
        }

        response.put("status", statusCode);
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
 */
