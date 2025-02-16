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
import java.util.logging.Level;
import java.util.logging.Logger;
@Singleton
@WebServlet("/user")
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
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);

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
        LOGGER.info("PUT /user/" + userId + " - Тіло запиту: " + body);

        Gson gson = new Gson();
        User updatedUser;
        try {
            updatedUser = gson.fromJson(body, User.class);
        } catch (JsonSyntaxException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Некоректний формат JSON\"}");
            return;
        }

        // Получаем пользователя из БД, обернув в try-catch на случай SQLException
        User existingUser;
        try {
            existingUser = userDao.getUserById(userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка при отриманні користувача id=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при отриманні користувача\"}");
            return;
        }

        // Если пользователя с таким id нет — 404
        if (existingUser == null) {
            sendJsonResponse(resp, 404, "{\"message\": \"Користувач не знайдений\"}");
            return;
        }

        // Обновляем поля
        existingUser.setName(updatedUser.getName());
        existingUser.setCity(updatedUser.getCity());
        existingUser.setAddress(updatedUser.getAddress());
        existingUser.setBirthdate(updatedUser.getBirthdate());
        existingUser.setEmails(updatedUser.getEmails());
        existingUser.setPhones(updatedUser.getPhones());

        // Пробуем сохранить изменения в БД
        try {
            userDao.updateUser(existingUser);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка при оновленні користувача id=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при оновленні користувача\"}");
            return;
        }

        // Достаём обновлённые данные из БД и возвращаем клиенту
        User refreshedUser;
        try {
            refreshedUser = userDao.getUserById(userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка при отриманні оновленого користувача id=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка при отриманні оновленого користувача\"}");
            return;
        }

        String json = gson.toJson(refreshedUser);
        sendJsonResponse(resp, 200, json);
    }
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);

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

        LOGGER.info("DELETE /user/" + userId);


        try {
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"Користувач не знайдений\"}");
                return;
            }

            userDao.deleteUser(userId);
            sendJsonResponse(resp, 200, "{\"message\": \"Користувача успішно видалено\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка бази даних при видаленні користувача", e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка бази даних\"}");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(jsonResponse);
    }
}
