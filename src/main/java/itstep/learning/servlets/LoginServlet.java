package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import itstep.learning.models.User;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
    private static final String CONNECTION_STRING = "jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "user221";
    private static final String DB_PASSWORD = "pass221";
    private final Gson gson = new Gson();

    // ✅ Додано метод GET (повертає помилку 405, оскільки GET не підтримується)
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        sendJsonResponse(resp, 405, Map.of("error", "Метод GET не підтримується. Використовуйте POST."));
    }

    // ✅ Метод POST для аутентифікації користувача
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("POST-запит на аутентифікацію отримано");
        setupResponseHeaders(resp);

        // ✅ Перевірка заголовка Authorization
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            sendJsonResponse(resp, 401, Map.of("error", "Відсутній заголовок Authorization"));
            return;
        }

        // ✅ Декодуємо `Authorization: Basic base64(login:password)`
        String base64Credentials = authHeader.substring("Basic ".length());
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        String[] parts = credentials.split(":", 2);

        if (parts.length != 2) {
            sendJsonResponse(resp, 400, Map.of("error", "Невірний формат логіна і пароля"));
            return;
        }

        String login = parts[0];
        String password = parts[1];

        try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD)) {
            LOGGER.info("Підключення до БД для авторизації");

            User user = getUserByLogin(connection, login);

            // ✅ Перевірка пароля
            if (user == null || !BCrypt.checkpw(password, user.getPassword())) {
                sendJsonResponse(resp, 401, Map.of("error", "Невірний логін або пароль"));
                return;
            }

            LOGGER.info("✅ Успішна авторизація користувача: " + login);

            // ✅ Генерація токена (можна використовувати JWT, тут просто `UUID`)
            String token = UUID.randomUUID().toString();

            sendJsonResponse(resp, 200, Map.of(
                    "message", "Успішний вхід",
                    "user_id", user.getId(),
                    "name", user.getName(),
                    "login", user.getLogin(),
                    "emails", user.getEmails(),
                    "phones", user.getPhones(),
                    "token", token
            ));
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "❌ Помилка бази даних при авторизації", ex);
            sendJsonResponse(resp, 500, Map.of("error", "Помилка бази даних"));
        }
    }

    // ✅ Отримання користувача за логіном
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

    // ✅ Дозволяємо CORS та OPTIONS-запити
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupResponseHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // ✅ Налаштування заголовків CORS
    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    // ✅ Універсальний метод для відправки JSON-відповіді
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, Map<String, Object> data) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(gson.toJson(data));
    }
}