
package itstep.learning.servlets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.dal.dao.UserDao;
import itstep.learning.models.User;
import itstep.learning.rest.RestResponse;
import itstep.learning.rest.RestService;
import itstep.learning.services.DbService.DbService;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.mindrot.jbcrypt.BCrypt;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private final Gson gson = new Gson();
    private UserDao userDao;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();

        // Из контекста Tomcat берем уже созданное Connection (если вы его там сохраняете)
        Connection connection = (Connection) context.getAttribute("dbConnection");
        Logger appLogger = (Logger) context.getAttribute("appLogger");

        // Создаём DAO
        userDao = new UserDao(connection, appLogger);
    }

    /**
     * GET: Получение списка пользователей
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("GET-запит отримано: повертаємо список користувачів");
        setupResponseHeaders(resp);

        List<User> users = userDao.getAllUsers(); // Получаем список пользователей из БД
        String jsonResponse = gson.toJson(users);
        sendJsonResponse(resp, 200, jsonResponse);
    }

    /**
     * POST: Регистрация нового пользователя
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("POST-запит на реєстрацію отримано");
        setupResponseHeaders(resp);

        // 1) Считываем тело запроса
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("🔎 [RegisterServlet] Тіло запиту: " + body);

        try {
            // 2) Парсим JSON в User
            User user = gson.fromJson(body, User.class);
            LOGGER.info("🔎 [RegisterServlet] Розпарсений користувач: " + user);

            // 3) Детально выводим поля
            LOGGER.info("🔎 user.name=" + user.getName()
                    + ", user.login=" + user.getLogin()
                    + ", user.emails=" + user.getEmails()
                    + ", user.phones=" + user.getPhones()
                    + ", user.city=" + user.getCity()
                    + ", user.address=" + user.getAddress()
                    + ", user.birthdate=" + user.getBirthdate()
                    + ", user.password (len)="
                    + (user.getPassword() == null ? 0 : user.getPassword().length()));

            // 4) Проверяем валидацию
            if (isUserDataInvalid(user)) {
                LOGGER.warning("❌ Невалідні дані користувача, відхиляємо запит.");
                sendJsonResponse(resp, 400, "{\"message\": \"Невалідні дані користувача\"}");
                return;
            }

            // 5) Хешируем пароль
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            user.setPassword(hashedPassword);

            // 6) Генерируем userId и вспомогательные поля
            long newUserId = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            user.setId(newUserId);
            user.setEmailConfirmed(false);
            user.setEmailConfirmationToken(UUID.randomUUID().toString());
            user.setTokenCreatedAt(new Timestamp(System.currentTimeMillis()));

            LOGGER.info("🔎 [RegisterServlet] Генерируем user_id=" + newUserId);

            // 7) Вызываем DAO-метод для сохранения в БД
            userDao.addUser(user);

            // 8) Отправляем ответ
            LOGGER.info("✅ [RegisterServlet] Користувач успішно зареєстрований!");
            sendJsonResponse(resp, 201, "{\"message\": \"Користувач успішно зареєстрований!\"}");

        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "❌ [RegisterServlet] Помилка парсингу JSON", e);
            sendJsonResponse(resp, 400, "{\"message\": \"Некоректний формат JSON\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ [RegisterServlet] Помилка бази даних", e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка бази даних\"}");
        }
    }

    /**
     * DELETE: Удаление пользователя
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("DELETE-запит на видалення користувача отримано");
        setupResponseHeaders(resp);

        String userIdStr = req.getParameter("id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            sendJsonResponse(resp, 400, "{\"message\": \"Не передано ID користувача\"}");
            return;
        }

        try {
            long userId = Long.parseLong(userIdStr);
            userDao.softDeleteUser(userId);
            sendJsonResponse(resp, 200, "{\"message\": \"Користувача успішно видалено\"}");
        } catch (NumberFormatException e) {
            sendJsonResponse(resp, 400, "{\"message\": \"Невірний формат ID\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка бази даних", e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка бази даних\"}");
        }
    }

    /**
     * Метод OPTIONS (CORS)
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Проверка валидности данных пользователя
     */
    private boolean isUserDataInvalid(User user) {
        return user == null
                || user.getName() == null || user.getName().isEmpty()
                || user.getLogin() == null || user.getLogin().isEmpty()
                || user.getEmails() == null || user.getEmails().isEmpty()
                || user.getPhones() == null || user.getPhones().isEmpty()
                || user.getBirthdate() == null || user.getBirthdate().isEmpty()
                || user.getPassword() == null || user.getPassword().length() < 6;
    }

    /**
     * Настройка заголовков CORS
     */
    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Max-Age", "3600");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    /**
     * Универсальный метод отправки JSON-ответа
     */
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(jsonResponse);
    }
}