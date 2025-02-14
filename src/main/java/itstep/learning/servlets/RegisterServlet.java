package itstep.learning.servlets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import itstep.learning.dal.dao.UserDao;
import itstep.learning.models.User;
import itstep.learning.rest.RestResponse;
import itstep.learning.rest.RestService;
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
        Connection connection = (Connection) context.getAttribute("dbConnection");
        Logger logger = (Logger) context.getAttribute("appLogger");
        userDao = new UserDao(connection, logger);
    }

    /**
     * ✅ Метод GET: Получение списка пользователей
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
     * ✅ Метод POST: Регистрация нового пользователя
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("POST-запит на реєстрацію отримано");
        setupResponseHeaders(resp);

        // Читаем тело запроса
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("Тіло запиту: " + body);

        try {
            User user = gson.fromJson(body, User.class);
            LOGGER.info("Розпарсений користувач: " + user);

            if (isUserDataInvalid(user)) {
                sendJsonResponse(resp, 400, "{\"message\": \"Невалідні дані користувача\"}");
                return;
            }

            // Хешируем пароль
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            user.setPassword(hashedPassword);

            // Генерируем UUID для user_id
            user.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
            user.setEmailConfirmed(false);
            user.setEmailConfirmationToken(UUID.randomUUID().toString());
            user.setTokenCreatedAt(new Timestamp(System.currentTimeMillis()));

            // Добавляем пользователя в БД
            userDao.addUser(user);

            LOGGER.info("Користувач успішно зареєстрований!");
            sendJsonResponse(resp, 201, "{\"message\": \"Користувач успішно зареєстрований!\"}");
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.WARNING, "Помилка парсингу JSON", e);
            sendJsonResponse(resp, 400, "{\"message\": \"Некоректний формат JSON\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Помилка бази даних", e);
            sendJsonResponse(resp, 500, "{\"message\": \"Помилка бази даних\"}");
        }
    }

    /**
     * Метод OPTIONS: Обрабатывает CORS-запросы
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
    public void addUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, login, city, address, birthdate, password) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", "user221", "pass221");
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getCity());
            stmt.setString(4, user.getAddress());
            stmt.setString(5, user.getBirthdate());
            stmt.setString(6, user.getPassword());
            stmt.executeUpdate();
        }
    }
    /**
     * Валидация данных пользователя
     */
    private boolean isUserDataInvalid(User user) {
        return user == null || user.getName() == null || user.getName().isEmpty()
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
