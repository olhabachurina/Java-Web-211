package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.models.User;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.DbService.MySqlDbService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String CONNECTION_STRING =
            "jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "user221";
    private static final String DB_PASSWORD = "pass221";
    private final Gson gson = new Gson();

    // Внедряем AccessTokenDao через DI (Guice)
    @Inject
    private AccessTokenDao accessTokenDao;



    @Override
    public void init() throws ServletException {
        super.init();

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("POST-запит на аутентифікацію отримано");
        setupResponseHeaders(resp);

        // Проверяем заголовок Authorization: Basic base64(login:password)
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendJsonResponse(resp, 401, Map.of("error", "Відсутній заголовок Authorization"));
            return;
        }

        // Декодируем Base64 (login:password)
        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            sendJsonResponse(resp, 400, Map.of("error", "Невірний формат логіна і пароля"));
            return;
        }

        String login = parts[0];
        String password = parts[1];

        // Подключаемся к БД
        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD)) {
            LOGGER.info("Підключення до БД для авторизації");

            User userShort = getUserByLogin(connection, login);
            if (userShort == null || !BCrypt.checkpw(password, userShort.getPassword())) {
                sendJsonResponse(resp, 401, Map.of("error", "Невірний логін або пароль"));
                return;
            }

            LOGGER.info("✅ Успішна авторизація користувача: " + login);

            User fullUser = getUserById(connection, userShort.getId());
            if (fullUser == null) {
                sendJsonResponse(resp, 500, Map.of("error", "Не вдалося отримати повні дані користувача"));
                return;
            }

            // Определяем время выпуска и истечения токена (например, на 7 дней)
            LocalDateTime issuedAt = LocalDateTime.now();
            LocalDateTime expiresAt = issuedAt.plusDays(7);

            String userIdStr = String.valueOf(fullUser.getId());
            String token;
            boolean tokenResult;
            String existingToken = accessTokenDao.getToken(userIdStr);
            if (existingToken != null) {
                // Есть активный токен – продлеваем его срок действия (не меняем сам token)
                tokenResult = accessTokenDao.updateToken(existingToken, userIdStr, issuedAt, expiresAt);
                token = existingToken;
                LOGGER.info("Продлено термін дії токена для user_id=" + userIdStr);
            } else {
                // Нет активного токена – генерируем и сохраняем новый
                token = UUID.randomUUID().toString();
                tokenResult = accessTokenDao.saveToken(token, userIdStr, issuedAt, expiresAt);
                LOGGER.info("Створено новий токен для user_id=" + userIdStr);
            }

            if (!tokenResult) {
                sendJsonResponse(resp, 500, Map.of("error", "Не вдалося створити/продовжити токен"));
                return;
            }

            Map<String, Object> responseData = new LinkedHashMap<>();
            responseData.put("message", "Успішний вхід");
            responseData.put("token", token);
            responseData.put("user_id", fullUser.getId());
            responseData.put("login", fullUser.getLogin());
            responseData.put("name", fullUser.getName());
            responseData.put("city", fullUser.getCity());
            responseData.put("address", fullUser.getAddress());
            responseData.put("birthdate", fullUser.getBirthdate());
            responseData.put("role", fullUser.getRole());
            responseData.put("emails", fullUser.getEmails());
            responseData.put("phones", fullUser.getPhones());

            sendJsonResponse(resp, 200, responseData);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "❌ Помилка бази даних при авторизації", ex);
            sendJsonResponse(resp, 500, Map.of("error", "Помилка бази даних"));
        }
    }

    private User getUserByLogin(Connection connection, String login) throws SQLException {
        String sql = "SELECT * FROM users WHERE login = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return User.fromResultSet(rs);
                }
            }
        }
        return null;
    }

    private User getUserById(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        User user = null;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user = User.fromResultSet(rs);
                    user.setEmails(getEmailsForUser(connection, userId));
                    user.setPhones(getPhonesForUser(connection, userId));
                }
            }
        }
        return user;
    }

    private java.util.List<String> getEmailsForUser(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT email FROM user_emails WHERE user_id = ?";
        java.util.List<String> emails = new java.util.ArrayList<>();
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

    private java.util.List<String> getPhonesForUser(Connection connection, Long userId) throws SQLException {
        String sql = "SELECT phone FROM user_phones WHERE user_id = ?";
        java.util.List<String> phones = new java.util.ArrayList<>();
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

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, Map<String, Object> data)
            throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(gson.toJson(data));
        LOGGER.info("Отправлен HTTP " + statusCode + ": " + gson.toJson(data));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        sendJsonResponse(resp, 405, Map.of("error", "Метод GET не підтримується. Використовуйте POST."));
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
