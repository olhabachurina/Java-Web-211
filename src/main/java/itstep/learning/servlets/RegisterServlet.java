package itstep.learning.servlets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import itstep.learning.models.User;
import itstep.learning.rest.RestResponse;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RegisterServlet.class.getName());
    private static final String CONNECTION_STRING = "jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "user221";
    private static final String DB_PASSWORD = "pass221";
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("POST-запит отримано");

        // Настройка CORS
        setupResponseHeaders(resp);

        // Логирование заголовков запроса
        req.getHeaderNames().asIterator().forEachRemaining(header -> {
            LOGGER.info("Заголовок: " + header + " = " + req.getHeader(header));
        });

        // Чтение тела запроса
        String body = new String(req.getInputStream().readAllBytes());
        LOGGER.info("Тіло запиту: " + body);

        try {
            // Парсинг JSON в объект User
            User user = gson.fromJson(body, User.class);
            LOGGER.info("Розпарсений об'єкт користувача: " + user);

            // Проверка валидности данных
            if (isUserDataInvalid(user)) {
                LOGGER.warning("Невалідні дані користувача: " + user);
                sendJsonResponse(resp, 400, "Невірні дані користувача");
                return;
            }

            // Хеширование пароля
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            user.setPassword(hashedPassword);
            LOGGER.info("Хешування пароля завершено");

            // Работа с базой данных
            try (Connection connection = DriverManager.getConnection(CONNECTION_STRING, DB_USER, DB_PASSWORD)) {
                connection.setAutoCommit(false); // Начало транзакции
                LOGGER.info("Підключення до бази даних встановлено");

                // Сохранение пользователя
                long userId = saveUserToDatabase(connection, user);

                // Сохранение email-адресов
                for (String email : user.getEmails()) {
                    saveEmailToDatabase(connection, userId, email);
                }

                // Сохранение телефонов
                for (String phone : user.getPhones()) {
                    savePhoneToDatabase(connection, userId, phone);
                }

                connection.commit(); // Завершение транзакции
                LOGGER.info("Транзакцію успішно завершено");
                sendJsonResponse(resp, 201, "Користувач успішно зареєстрований!");
            }
        } catch (JsonSyntaxException ex) {
            LOGGER.log(Level.WARNING, "Помилка парсингу JSON", ex);
            sendJsonResponse(resp, 400, "Некоректний формат JSON");
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Помилка бази даних", ex);
            sendJsonResponse(resp, 500, "Помилка бази даних");
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Невідома помилка сервера", ex);
            sendJsonResponse(resp, 500, "Помилка сервера");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp); // Настройка CORS для preflight-запросов
        LOGGER.info("Оброблено preflight-запит (OPTIONS)");
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // Метод для проверки данных пользователя
    private boolean isUserDataInvalid(User user) {
        if (user == null) {
            LOGGER.warning("Користувач є null");
            return true;
        }
        if (user.getName() == null || user.getName().isEmpty()) {
            LOGGER.warning("Ім'я користувача порожнє або null");
            return true;
        }
        if (user.getLogin() == null || user.getLogin().isEmpty()) {
            LOGGER.warning("Логін користувача порожній або null");
            return true;
        }
        if (user.getEmails() == null || user.getEmails().isEmpty() ||
                user.getEmails().stream().anyMatch(email -> email == null || email.isEmpty())) {
            LOGGER.warning("Список email користувача порожній або містить порожні значення");
            return true;
        }
        if (user.getPhones() == null || user.getPhones().isEmpty() ||
                user.getPhones().stream().anyMatch(phone -> phone == null || phone.isEmpty())) {
            LOGGER.warning("Список телефонів користувача порожній або містить порожні значення");
            return true;
        }
        if (!isBirthdateValid(user.getBirthdate())) {
            LOGGER.warning("Некоректна дата народження: " + user.getBirthdate());
            return true;
        }
        if (user.getPassword() == null || user.getPassword().isEmpty() || user.getPassword().length() < 6) {
            LOGGER.warning("Пароль користувача порожній або занадто короткий");
            return true;
        }
        return false;
    }

    // Метод для проверки формата даты
    private boolean isBirthdateValid(String birthdate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(birthdate);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Метод для сохранения пользователя в базу данных
    private long saveUserToDatabase(Connection connection, User user) throws SQLException {
        String query = "INSERT INTO users (name, login, city, address, birthdate, password) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, user.getName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getCity());
            statement.setString(4, user.getAddress());
            statement.setString(5, user.getBirthdate());
            statement.setString(6, user.getPassword());
            statement.executeUpdate();

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                throw new SQLException("Не вдалося отримати ID нового користувача");
            }
        }
    }

    // Метод для сохранения email в базу данных
    private void saveEmailToDatabase(Connection connection, long userId, String email) throws SQLException {
        String query = "INSERT INTO user_emails (user_id, email) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setString(2, email);
            statement.executeUpdate();
        }
    }

    // Метод для сохранения телефона в базу данных
    private void savePhoneToDatabase(Connection connection, long userId, String phone) throws SQLException {
        String query = "INSERT INTO user_phones (user_id, phone) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setLong(1, userId);
            statement.setString(2, phone);
            statement.executeUpdate();
        }
    }

    // Метод для настройки CORS
    private void setupResponseHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
    }

    // Метод для отправки JSON-ответа
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String message) throws IOException {
        resp.setStatus(statusCode);
        try (PrintWriter writer = resp.getWriter()) {
            writer.print("{\"message\": \"" + message + "\"}");
        }
    }

}