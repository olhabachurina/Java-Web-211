package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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

@Singleton
@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    private final DbService dbService;
    private final RandomService randomService;
    private final DateTimeService dateTimeService;
    private final KdfService kdfService;

    // Конструктор с аннотацией @Inject (Guice)
    @Inject
    public HomeServlet(DbService dbService, RandomService randomService, DateTimeService dateTimeService, KdfService kdfService) {
        this.dbService = dbService;
        this.randomService = randomService;
        this.dateTimeService = dateTimeService;
        this.kdfService = kdfService;
    }

    // Конструктор по умолчанию (необходим для Tomcat)
    public HomeServlet() {
        this.dbService = null;
        this.randomService = null;
        this.dateTimeService = null;
        this.kdfService = null;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCorsHeaders(resp);

        String currentTime = null;
        String databases = null;
        int randomNumber = randomService != null ? randomService.randomInt() : -1; // Защита от NullPointerException
        String hashedMessage = kdfService != null ? kdfService.dk("123", "456") : "null";
        String message;

        try (Connection connection = dbService != null ? dbService.getConnection() : null) {
            if (connection != null) {
                currentTime = fetchCurrentTime(connection);
                databases = fetchDatabases(connection);
                message = "Запит виконано успішно. Згенероване випадкове число: " + randomNumber;
            } else {
                message = "Не вдалося встановити з'єднання з базою даних.";
            }
        } catch (SQLException e) {
            message = "Помилка бази даних: " + e.getMessage();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("currentTime", currentTime);
        response.put("databases", databases);
        response.put("randomNumber", randomNumber);
        response.put("message", message);
        response.put("status", 200);
        response.put("hashedMessage", hashedMessage);

        resp.getWriter().print(new Gson().toJson(response));
    }

    private String fetchCurrentTime(Connection connection) throws SQLException {
        String query = "SELECT CURRENT_TIMESTAMP";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    private String fetchDatabases(Connection connection) throws SQLException {
        String query = "SHOW DATABASES";
        StringBuilder dbBuilder = new StringBuilder();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                if (dbBuilder.length() > 0) {
                    dbBuilder.append(", ");
                }
                dbBuilder.append(resultSet.getString(1));
            }
        }
        return dbBuilder.toString();
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
