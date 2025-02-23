package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import itstep.learning.dal.dao.UserDao;
import itstep.learning.models.User;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mysql.cj.conf.PropertyKey.logger;

@Singleton
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    private UserDao userDao;
    private Connection connection;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();

        this.connection = (Connection) context.getAttribute("dbConnection");
        Logger appLogger = (Logger) context.getAttribute("appLogger");

        userDao = new UserDao(connection, appLogger);
        LOGGER.info("UserServlet инициализирован.");
    }

    /**
     * PUT /users/{id}
     * Асинхронное обновление данных пользователя (users, телефоны) и user_access (логин).
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("Получен PUT-запрос: " + req.getRequestURI());

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"User ID not provided in URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
            LOGGER.info("Парсинг userId: " + userId);
        } catch (NumberFormatException e) {
            LOGGER.warning("Неверный формат user_id в URL: " + pathInfo);
            sendJsonResponse(resp, 400, "{\"message\": \"Invalid user_id format\"}");
            return;
        }

        // Считываем тело запроса (JSON)
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("Request body: " + body);

        Gson gson = new Gson();
        User updatedUser;
        try {
            updatedUser = gson.fromJson(body, User.class);
            LOGGER.info("Parsed updatedUser: " + gson.toJson(updatedUser));
        } catch (JsonSyntaxException e) {
            LOGGER.warning("Incorrect JSON format: " + e.getMessage());
            sendJsonResponse(resp, 422, "{\"message\": \"Incorrect JSON format\"}");
            return;
        }

        try {
            // Получаем существующего пользователя
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                LOGGER.warning("User with ID " + userId + " not found.");
                sendJsonResponse(resp, 404, "{\"message\": \"User not found\"}");
                return;
            }

            LOGGER.info("Начинаем обновление для user ID=" + userId);
            LOGGER.info("Старые данные: " + gson.toJson(existingUser));

            // Обновляем поля, если они переданы
            if (updatedUser.getName() != null) {
                existingUser.setName(updatedUser.getName());
                LOGGER.info("Updated name to: " + updatedUser.getName());
            }
            if (updatedUser.getCity() != null) {
                existingUser.setCity(updatedUser.getCity());
                LOGGER.info("Updated city to: " + updatedUser.getCity());
            }
            if (updatedUser.getAddress() != null) {
                existingUser.setAddress(updatedUser.getAddress());
                LOGGER.info("Updated address to: " + updatedUser.getAddress());
            }
            if (updatedUser.getBirthdate() != null) {
                existingUser.setBirthdate(updatedUser.getBirthdate());
                LOGGER.info("Updated birthdate to: " + updatedUser.getBirthdate());
            }

            // Если пришёл новый email, обновляем login в users (и далее в users_access)
            String newEmail = null;
            if (updatedUser.getEmails() != null && !updatedUser.getEmails().isEmpty()) {
                newEmail = updatedUser.getEmails().get(0);
                existingUser.setLogin(newEmail);
                LOGGER.info("Updated login (email) to: " + newEmail);
            }

            final long finalUserId = userId;
            final User finalExistingUser = existingUser;
            final String finalNewEmail = newEmail;

            // Асинхронное обновление данных пользователя (users + телефоны)
            CompletableFuture<Void> userUpdateFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (updatedUser.getPhones() != null) {
                        LOGGER.info("[Async] Updating phones for user ID=" + finalUserId
                                + ". Phones: " + updatedUser.getPhones());
                        userDao.updateUserPhones(finalUserId, updatedUser.getPhones());
                    }
                    userDao.updateUser(finalExistingUser);
                    LOGGER.info("[Async] User data updated for user ID=" + finalUserId);
                } catch (SQLException e) {
                    LOGGER.severe("[Async] Error updating user data for user ID=" + finalUserId + ": " + e.getMessage());
                    throw new RuntimeException("Error updating user (ID=" + finalUserId + "): " + e.getMessage(), e);
                }
            });

            // Асинхронное обновление данных доступа (таблица users_access, поле login)
            CompletableFuture<Void> userAccessFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (finalNewEmail != null && !finalNewEmail.isEmpty()) {
                        LOGGER.info("[Async] Updating user access login for user ID=" + finalUserId
                                + " to new login: " + finalNewEmail);
                        userDao.updateUserAccessLogin(finalUserId, finalNewEmail);
                        LOGGER.info("[Async] User access login updated for user ID=" + finalUserId);
                    } else {
                        LOGGER.info("[Async] No new email provided to update user access login for user ID=" + finalUserId);
                    }
                } catch (SQLException e) {
                    LOGGER.severe("[Async] Error updating user access for user ID=" + finalUserId + ": " + e.getMessage());
                    throw new RuntimeException("Error updating user access (ID=" + finalUserId + "): " + e.getMessage(), e);
                }
            });

            LOGGER.info("Waiting for asynchronous tasks to complete...");
            CompletableFuture.allOf(userUpdateFuture, userAccessFuture).join();
            LOGGER.info("Asynchronous tasks completed.");

            // Получаем обновлённые данные (с полными полями: email, phones и т.д.)
            User refreshedUser = userDao.getUserDetailsById(finalUserId);
            LOGGER.info("Updated user (and access) details: " + gson.toJson(refreshedUser));

            sendJsonResponse(resp, 200, gson.toJson(refreshedUser));

        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            LOGGER.log(Level.SEVERE, "[Async] Error updating user ID=" + userId, cause);
            sendJsonResponse(resp, 500, "{\"message\": \"Error during asynchronous update: " + cause.getMessage() + "\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user ID=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Error updating user: " + e.getMessage() + "\"}");
        }
    }

    /**
     * GET /users/{id}
     * Получение данных пользователя (id, name, login).
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("Получен GET-запрос: " + req.getRequestURI());

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"User ID not provided in URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Invalid user_id format\"}");
            return;
        }

        try {
            // Если нужно вернуть полные данные (email, phones), используйте getUserDetailsById
            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"User not found\"}");
                return;
            }

            Gson gson = new Gson();
            sendJsonResponse(resp, 200, gson.toJson(user));
        } catch (SQLException e) {
            sendJsonResponse(resp, 500, "{\"message\": \"Database error\"}");
        }
    }

    /**
     * DELETE /users/{id}
     * Мягкое удаление (soft delete) пользователя.
     */

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);

        String pathInfo = req.getPathInfo(); // наприклад, /36
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"Не вказано user_id у URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
            LOGGER.info("Розібрано userId для видалення: " + userId);
        } catch (NumberFormatException e) {
            LOGGER.warning("Невірний формат user_id у URL: " + pathInfo);
            sendJsonResponse(resp, 400, "{\"message\": \"Неправильний формат user_id\"}");
            return;
        }

        try {
            User user = userDao.getUserById(userId);
            if (user == null) {
                LOGGER.warning("Користувача з ID " + userId + " не знайдено для видалення.");
                sendJsonResponse(resp, 404, "{\"message\": \"Користувача не знайдено\"}");
                return;
            }

            LOGGER.info("Запуск асинхронного м'якого видалення для користувача з ID=" + userId);
            CompletableFuture<Void> deleteFuture = CompletableFuture.runAsync(() -> {
                try {
                    userDao.softDeleteUser(userId);
                    LOGGER.info("[Async] Користувач з ID=" + userId + " успішно анонімізований (soft delete).");
                } catch (SQLException e) {
                    LOGGER.severe("[Async] Помилка м'якого видалення для користувача з ID=" + userId + ": " + e.getMessage());
                    throw new RuntimeException("Помилка soft delete для користувача з ID=" + userId + ": " + e.getMessage(), e);
                }
            });

            LOGGER.info("Очікування завершення асинхронної операції м'якого видалення...");
            deleteFuture.join();
            LOGGER.info("Асинхронне м'яке видалення завершено для користувача з ID=" + userId);

            sendJsonResponse(resp, 200, "{\"message\": \"Користувача успішно анонімізовано і позначено як видаленого\"}");
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            LOGGER.log(Level.SEVERE, "[Async] Помилка при виконанні soft delete для користувача з ID=" + userId, cause);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при видаленні (async): " + cause.getMessage() + "\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка видалення користувача з ID=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка видалення: " + e.getMessage() + "\"}");
        }
    }

    // Проверка существования пользователя (дублирует userDao?)
    private boolean isUserExists(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * CORS + заголовки
     */
    private void setupResponseHeaders(HttpServletResponse resp) {
        // Замените на ваш фронт (если credentials: true, то нельзя указывать "*")
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Универсальный метод отправки JSON-ответа
     */
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(jsonResponse);
        LOGGER.info("Отправлен HTTP " + statusCode + ": " + jsonResponse);
    }
}