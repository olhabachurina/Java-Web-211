package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
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
    private final Gson gson = new Gson(); // Для створення JSON-відповідей
    private final RandomService randomService; // Сервіс для генерації випадкових чисел
    String connectionString = "jdbc:mysql://localhost:3306/Java221?useSSL=false&serverTimezone=UTC";

    // Ін’єкція RandomService через Guice
    @Inject
    public HomeServlet(RandomService randomService) {
        this.randomService = randomService;
    }

    // Конструктор за замовчуванням (вимагається контейнером сервлетів)
    public HomeServlet() {
        this.randomService = null; // Тимчасове значення, Guice виконає ін’єкцію
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String message;
        int status;
        String currentTime = null;
        String databases = null; // Для збереження списку баз даних
        int randomNumber = randomService.randomInt(); // Генерація випадкового числа

        // Налаштування CORS
        resp.setContentType("application/json;charset=UTF-8");
        setCorsHeaders(resp);

        try {
            // Підключення драйвера MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Підключення до бази даних
            try (Connection connection = DriverManager.getConnection(connectionString, "user221", "pass221")) {
                if (connection != null) {
                    // 1. Отримання поточного часу
                    currentTime = fetchCurrentTime(connection);

                    // 2. Отримання списку баз даних
                    databases = fetchDatabases(connection);

                    message = "Запит виконано успішно. Згенероване випадкове число: " + randomNumber;
                    status = 200;
                } else {
                    message = "З'єднання дорівнює null. Згенероване випадкове число: " + randomNumber;
                    status = 500;
                }
            }
        } catch (ClassNotFoundException ex) {
            message = "JDBC драйвер не знайдено: " + ex.getMessage();
            status = 500;
        } catch (SQLException ex) {
            message = "Помилка бази даних: " + ex.getMessage();
            status = 500;
        }

        // Формування JSON-відповіді
        sendJsonResponse(resp, status, message, currentTime, databases, randomNumber);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setCorsHeaders(resp); // Налаштування заголовків для preflight-запитів
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Встановлює CORS-заголовки.
     */
    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Отримує поточний час з бази даних.
     */
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

    /**
     * Отримує список баз даних.
     */
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

    /**
     * Відправляє JSON-відповідь клієнту.
     */
    private void sendJsonResponse(HttpServletResponse resp, int status, String message, String currentTime, String databases, int randomNumber) throws IOException {
        Map<String, Object> response = new HashMap<>();
        response.put("status", status);
        response.put("message", message);
        response.put("currentTime", currentTime);
        response.put("databases", databases);
        response.put("randomNumber", randomNumber);

        resp.getWriter().print(gson.toJson(response));
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
