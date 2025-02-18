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
        LOGGER.info("✅ UserServlet ініціалізовано");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("📥 Отримано PUT-запит: " + req.getRequestURI());

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
            // 🛠️ Сначала получаем существующего пользователя
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"Користувач не знайдений\"}");
                return;
            }

            LOGGER.info("🔄 Оновлення користувача ID=" + userId);
            LOGGER.info("➡ Старі дані: " + gson.toJson(existingUser));

            // 🛠️ Обновляем только переданные поля
            if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
            if (updatedUser.getCity() != null) existingUser.setCity(updatedUser.getCity());
            if (updatedUser.getAddress() != null) existingUser.setAddress(updatedUser.getAddress());
            if (updatedUser.getBirthdate() != null) existingUser.setBirthdate(updatedUser.getBirthdate());

            // 🛠️ Обновляем телефоны отдельно
            if (updatedUser.getPhones() != null) {
                userDao.updateUserPhones(userId, updatedUser.getPhones());
            }

            // 🛠️ Вызываем `updateUser()`, который НЕ должен содержать `password`
            userDao.updateUser(existingUser);

            // 🛠️ Получаем обновленные данные
            User refreshedUser = userDao.getUserById(userId);
            LOGGER.info("✅ Оновлено користувача: " + gson.toJson(refreshedUser));

            sendJsonResponse(resp, 200, gson.toJson(refreshedUser));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Помилка при оновленні користувача ID=" + userId, e);
            e.printStackTrace();
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при оновленні користувача: " + e.getMessage() + "\"}");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("📥 Отримано GET-запит: " + req.getRequestURI());

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
        LOGGER.info("📤 Відправлено HTTP " + statusCode + ": " + jsonResponse);
    }
}