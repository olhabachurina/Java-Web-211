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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
        LOGGER.info(" UserServlet ініціалізовано");
    }

    /*@Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info(" Отримано PUT-запит: " + req.getRequestURI());

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"Не вказано user_id у URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Неправильний формат user_id\"}");
            return;
        }

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Gson gson = new Gson();

        User updatedUser;
        try {
            updatedUser = gson.fromJson(body, User.class);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, 422, "{\"message\": \"Некоректний формат JSON\"}");
            return;
        }

        try {
            //  Сначала получаем существующего пользователя
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"Користувач не знайдений\"}");
                return;
            }

            LOGGER.info(" Оновлення користувача ID=" + userId);
            LOGGER.info("➡ Старі дані: " + gson.toJson(existingUser));

            //  Обновляем только переданные поля
            if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
            if (updatedUser.getCity() != null) existingUser.setCity(updatedUser.getCity());
            if (updatedUser.getAddress() != null) existingUser.setAddress(updatedUser.getAddress());
            if (updatedUser.getBirthdate() != null) existingUser.setBirthdate(updatedUser.getBirthdate());

            //  Обновляем телефоны отдельно
            if (updatedUser.getPhones() != null) {
                userDao.updateUserPhones(userId, updatedUser.getPhones());
            }

            //  Вызываем `updateUser()`, который НЕ должен содержать `password`
            userDao.updateUser(existingUser);

            //  Получаем обновленные данные
            User refreshedUser = userDao.getUserById(userId);
            LOGGER.info(" Оновлено користувача: " + gson.toJson(refreshedUser));

            sendJsonResponse(resp, 200, gson.toJson(refreshedUser));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, " Помилка при оновленні користувача ID=" + userId, e);
            e.printStackTrace();
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при оновленні користувача: " + e.getMessage() + "\"}");
        }
    }*/
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info(" Received PUT request: " + req.getRequestURI());

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"User ID not provided in URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
            LOGGER.info("Parsed userId: " + userId);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid user_id format in URL: " + pathInfo);
            sendJsonResponse(resp, 400, "{\"message\": \"Invalid user_id format\"}");
            return;
        }

        // Читаемо тіло запроса
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
            // Отримаємо текущего користувача из БД
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                LOGGER.warning("User with ID " + userId + " not found.");
                sendJsonResponse(resp, 404, "{\"message\": \"User not found\"}");
                return;
            }

            LOGGER.info("Starting update for user ID=" + userId);
            LOGGER.info("Existing user data: " + gson.toJson(existingUser));

            // оновлюємо поля, якщо вони передани в запросе
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

            // якщо передан новий email, оновлюємо поле login у таблиці users
            String newEmail = null;
            if (updatedUser.getEmails() != null && !updatedUser.getEmails().isEmpty()) {
                newEmail = updatedUser.getEmails().get(0);
                existingUser.setLogin(newEmail);
                LOGGER.info("Updated login (email) to: " + newEmail);
            }

            //  final  для використання в лямбдах
            final long finalUserId = userId;
            final User finalExistingUser = existingUser;
            final String finalNewEmail = newEmail;

            // Асінхронне обновлення даних користувача (таблиці users, телефони и т.д.)
            CompletableFuture<Void> userUpdateFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (updatedUser.getPhones() != null) {
                        LOGGER.info("[Async] Updating phones for user ID=" + finalUserId + ". Phones: " + updatedUser.getPhones());
                        userDao.updateUserPhones(finalUserId, updatedUser.getPhones());
                    }
                    userDao.updateUser(finalExistingUser);
                    LOGGER.info("[Async] User data updated for user ID=" + finalUserId);
                } catch (SQLException e) {
                    LOGGER.severe("[Async] Error updating user data for user ID=" + finalUserId + ": " + e.getMessage());
                    throw new RuntimeException("Error updating user (ID=" + finalUserId + "): " + e.getMessage(), e);
                }
            });

            // Асінхронне обновлення даних доступа (таблиця users_access)
            CompletableFuture<Void> userAccessFuture = CompletableFuture.runAsync(() -> {
                try {
                    if (finalNewEmail != null && !finalNewEmail.isEmpty()) {
                        LOGGER.info("[Async] Updating user access login for user ID=" + finalUserId + " to new login: " + finalNewEmail);
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
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info(" Отримано GET-запит: " + req.getRequestURI());

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"Не вказано user_id у URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Неправильний формат user_id\"}");
            return;
        }

        try {
            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"Користувач не знайдений\"}");
                return;
            }

            Gson gson = new Gson();
            sendJsonResponse(resp, 200, gson.toJson(user));
        } catch (SQLException e) {
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка бази даних\"}");
        }
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);

        String pathInfo = req.getPathInfo(); // /10
        if (pathInfo == null || pathInfo.length() < 2) {
            sendJsonResponse(resp, 400, "{\"message\": \"Не указан user_id в URL\"}");
            return;
        }

        long userId;
        try {
            userId = Long.parseLong(pathInfo.substring(1));
        } catch (NumberFormatException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Неправильный формат user_id\"}");
            return;
        }

        try {

            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"Пользователь не найден\"}");
                return;
            }

            // Удаляем
            userDao.deleteUser(userId);
            sendJsonResponse(resp, 200, "{\"message\": \"Пользователь успешно удалён\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            sendJsonResponse(resp, 500, "{\"message\": \"Ошибка удаления: " + e.getMessage() + "\"}");
        }
    }
    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");  // Замените на ваш фронт
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
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(jsonResponse);
        LOGGER.info(" Відправлено HTTP " + statusCode + ": " + jsonResponse);
    }
}