package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.models.User;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.DbService.MySqlDbService;
import itstep.learning.services.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Key;



@Singleton
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String CONNECTION_STRING =
            "jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "user221";
    private static final String DB_PASSWORD = "pass221";

    private final Gson gson = new Gson();

    @Inject
    private JwtService jwtService;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("🔐 Получен POST-запрос на аутентификацию");
        setupResponseHeaders(resp);

        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendJsonResponse(resp, 401, Map.of("error", "⛔ Отсутствует заголовок Authorization"));
            return;
        }

        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2) {
            sendJsonResponse(resp, 400, Map.of("error", "⛔ Неверный формат логина и пароля"));
            return;
        }

        String login = parts[0];
        String password = parts[1];

        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD)) {
            LOGGER.info("✅ Установлено подключение к БД для авторизации");

            // Получаем пользователя по логину
            User userShort = getUserByLogin(connection, login);
            if (userShort == null) {
                sendJsonResponse(resp, 401, Map.of("error", "⛔ Неверный логин или пользователь не найден"));
                return;
            }

            // Проверка пароля
            if (!BCrypt.checkpw(password, userShort.getPassword())) {
                sendJsonResponse(resp, 401, Map.of("error", "⛔ Неверный пароль"));
                return;
            }

            // Подгружаем полные данные пользователя (включая role)
            User fullUser = getUserById(connection, userShort.getId());
            if (fullUser == null) {
                sendJsonResponse(resp, 500, Map.of("error", "⛔ Не удалось получить полные данные пользователя"));
                return;
            }

            // ✅ ПРОВЕРКА РОЛИ
            if (fullUser.getRole() == null || fullUser.getRole().isBlank()) {
                LOGGER.warning("⛔ Пользователь без роли: " + login);
                sendJsonResponse(resp, 403, Map.of("error", "Роль не установлена. Обратитесь к администратору."));
                return;
            }

            LOGGER.info("✅ Успешная авторизация пользователя: " + login + " с ролью: " + fullUser.getRole());

            // Формирование payload для JWT
            JsonObject payload = new JsonObject();
            payload.addProperty("user_id", fullUser.getId());
            payload.addProperty("login", fullUser.getLogin());
            payload.addProperty("role", fullUser.getRole());

            String jwtToken = jwtService.createJwt(payload);

            Map<String, Object> responseData = Map.of(
                    "message", "Успешный вход",
                    "token", jwtToken,
                    "user", fullUser
            );

            sendJsonResponse(resp, 200, responseData);

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "❌ Ошибка базы данных при авторизации", ex);
            sendJsonResponse(resp, 500, Map.of("error", "Ошибка базы данных"));
        }
    }

    private User getUserByLogin(Connection connection, String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return User.fromResultSet(rs); // ✅ Убедись, что сюда попадает password
                }
            }
        }
        return null;
    }

    private User getUserById(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT u.*, ua.role_id FROM users u " +
                "LEFT JOIN users_access ua ON u.id = ua.user_id " +
                "WHERE u.id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = User.fromResultSet(rs); // ✅ Здесь также учитывай role
                    user.setRole(rs.getString("role_id"));

                    // Загружаем emails и телефоны
                    user.setEmails(getUserEmails(connection, userId));
                    user.setPhones(getUserPhones(connection, userId));

                    LOGGER.info("🔍 Загружены данные пользователя ID=" + userId +
                            ": Role=" + user.getRole() +
                            ", Emails=" + user.getEmails() +
                            ", Phones=" + user.getPhones());

                    return user;
                }
            }
        }
        return null;
    }

    private List<String> getUserEmails(Connection connection, Long userId) throws SQLException {
        List<String> emails = new ArrayList<>();
        String sql = "SELECT email FROM user_emails WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString("email"));
                }
            }
        }
        return emails;
    }

    private List<String> getUserPhones(Connection connection, Long userId) throws SQLException {
        List<String> phones = new ArrayList<>();
        String sql = "SELECT phone FROM user_phones WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    phones.add(rs.getString("phone"));
                }
            }
        }
        return phones;
    }

    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, Object data) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(data));
        LOGGER.info("📤 Ответ отправлен [" + statusCode + "]: " + gson.toJson(data));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        sendJsonResponse(resp, 405, Map.of("error", "Метод GET не поддерживается. Используйте POST."));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
