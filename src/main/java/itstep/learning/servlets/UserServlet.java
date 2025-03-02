package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.dal.dao.UserDao;
import itstep.learning.models.User;
import itstep.learning.services.JwtService;
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
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mysql.cj.conf.PropertyKey.logger;



import static com.mysql.cj.conf.PropertyKey.logger;
@Singleton
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    private UserDao userDao;
    private Connection connection;

    @Inject
    private JwtService jwtService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        this.connection = (Connection) context.getAttribute("dbConnection");
        Logger appLogger = (Logger) context.getAttribute("appLogger");
        userDao = new UserDao(connection, appLogger);
        LOGGER.info("UserServlet инициализирован.");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("Получен PUT-запрос: " + req.getRequestURI());

        String token = extractBearerToken(req);
        if (token == null) {
            sendJsonResponse(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }
        JsonElement payload = jwtService.fromJwt(token);
        if (payload == null) {
            sendJsonResponse(resp, 403, Map.of("error", "Invalid or expired token"));
            return;
        }

        long userId = payload.getAsJsonObject().get("user_id").getAsLong();
        LOGGER.info("Авторизован пользователь с ID: " + userId);

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Gson gson = new Gson();
        User updatedUser;
        try {
            updatedUser = gson.fromJson(body, User.class);
        } catch (Exception e) {
            sendJsonResponse(resp, 422, Map.of("error", "Incorrect JSON format"));
            return;
        }

        try {
            User existingUser = userDao.getUserById(userId);
            if (existingUser == null) {
                sendJsonResponse(resp, 404, Map.of("error", "User not found"));
                return;
            }

            if (updatedUser.getName() != null) existingUser.setName(updatedUser.getName());
            if (updatedUser.getCity() != null) existingUser.setCity(updatedUser.getCity());
            if (updatedUser.getAddress() != null) existingUser.setAddress(updatedUser.getAddress());
            if (updatedUser.getBirthdate() != null) existingUser.setBirthdate(updatedUser.getBirthdate());

            userDao.updateUser(existingUser);

            sendJsonResponse(resp, 200, Map.of("message", "User updated successfully", "user", existingUser));

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Ошибка при обновлении пользователя с ID=" + userId, e);
            sendJsonResponse(resp, 500, Map.of("error", "Database error"));
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        String token = extractBearerToken(req);
        if (token == null) {
            sendJsonResponse(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }
        JsonElement payload = jwtService.fromJwt(token);
        if (payload == null) {
            sendJsonResponse(resp, 403, Map.of("error", "Invalid or expired token"));
            return;
        }
        long userId = payload.getAsJsonObject().get("user_id").getAsLong();
        LOGGER.info("Авторизован пользователь с ID: " + userId);
        try {
            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, Map.of("error", "User not found"));
                return;
            }
            sendJsonResponse(resp, 200, user);
        } catch (SQLException e) {
            sendJsonResponse(resp, 500, Map.of("error", "Database error"));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        String token = extractBearerToken(req);
        if (token == null) {
            sendJsonResponse(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }
        JsonElement payload = jwtService.fromJwt(token);
        if (payload == null) {
            sendJsonResponse(resp, 403, Map.of("error", "Invalid or expired token"));
            return;
        }
        long userId = payload.getAsJsonObject().get("user_id").getAsLong();
        LOGGER.info("Авторизован пользователь с ID: " + userId);
        try {
            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, Map.of("error", "User not found"));
                return;
            }
            userDao.softDeleteUser(userId);
            sendJsonResponse(resp, 200, Map.of("message", "User deleted successfully"));
        } catch (SQLException e) {
            sendJsonResponse(resp, 500, Map.of("error", "Database error"));
        }
    }

    private String extractBearerToken(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void setupResponseHeaders(HttpServletResponse resp) {
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

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, Object data) throws IOException {
        Gson gson = new Gson();
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(gson.toJson(data));
        LOGGER.info("Отправлен HTTP " + statusCode + ": " + gson.toJson(data));
    }
}

/*@Singleton
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());

    private UserDao userDao;
    private Connection connection;

    // Guice-внедрение DAO для работы с токенами
    @Inject
    private AccessTokenDao accessTokenDao;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();

        // Достаём готовое подключение к БД и логгер
        this.connection = (Connection) context.getAttribute("dbConnection");
        Logger appLogger = (Logger) context.getAttribute("appLogger");

        // Инициализируем UserDao
        userDao = new UserDao(connection, appLogger);
        LOGGER.info("UserServlet инициализирован.");
    }

    /**
     * PUT /users/{id}
     * Асинхронное обновление данных пользователя (users, телефоны) и user_access (логин).

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("Получен PUT-запрос: " + req.getRequestURI());

        // Проверяем наличие Access Token в заголовке
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(resp, 401, "{\"message\": \"Access token is missing or invalid\"}");
            return;
        }
        String accessToken = authHeader.substring(7); // Убираем "Bearer "

        //  Получаем userId из URL
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

        //  Проверяем валидность токена
        boolean isValid = accessTokenDao.isTokenValid(accessToken, String.valueOf(userId));


        //  Считываем тело запроса (JSON)
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
            // 5️⃣ Получаем существующего пользователя
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

            //  Генерируем новый Access Token и рассчитываем срок его действия
            String userIdStr = String.valueOf(finalUserId);
            String newAccessToken = UUID.randomUUID().toString();
            LocalDateTime issuedAt = LocalDateTime.now();
            LocalDateTime expiresAt = issuedAt.plusDays(7);
            LOGGER.info("🔑 Сгенерирован новий Access Token: " + newAccessToken);

            //  «Обновить или создать» запись в access_tokens
            String existingToken = accessTokenDao.getToken(userIdStr);
            boolean tokenOperationResult;
            if (existingToken == null) {
                // Записи нет – создаём новую
                tokenOperationResult = accessTokenDao.saveToken(newAccessToken, userIdStr, issuedAt, expiresAt);
                if (tokenOperationResult) {
                    LOGGER.info("✅ Новый токен сохранён в БД для user_id=" + finalUserId);
                }
            } else {
                // Запись есть – обновляем (продлеваем срок действия)
                tokenOperationResult = accessTokenDao.updateToken(existingToken, userIdStr, issuedAt, expiresAt);
                if (tokenOperationResult) {
                    LOGGER.info("✅ Токен обновлён для user_id=" + finalUserId);
                }
            }

            if (!tokenOperationResult) {
                LOGGER.warning("❌ Не удалось обновить/сохранить токен.");
                sendJsonResponse(resp, 500, "{\"message\": \"Помилка оновлення токена\"}");
                return;
            }

            // 8️⃣ Получаем обновленные данные (с полными полями: email, phones и т.д.)
            User refreshedUser = userDao.getUserDetailsById(finalUserId);
            LOGGER.info("Updated user (and access) details: " + gson.toJson(refreshedUser));

            // 9️⃣ Отправляем обновленные данные + новый токен
            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("message", "Користувач успішно оновлений");
            responseJson.addProperty("token", newAccessToken);
            responseJson.add("user", gson.toJsonTree(refreshedUser));

            sendJsonResponse(resp, 200, responseJson.toString());

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(resp, 401, "{\"message\": \"Access token is missing or invalid\"}");
            return;
        }
        String token = authHeader.substring(7);

        // Получаем userId из URL
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

        // Проверяем валидность токена для данного пользователя
        boolean valid;
        valid = accessTokenDao.isTokenValid(token, String.valueOf(userId));
        if (!valid) {
            sendJsonResponse(resp, 403, "{\"message\": \"Invalid or expired token\"}");
            return;
        }

        // Если токен валиден – возвращаем данные пользователя
        try {
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        String pathInfo = req.getPathInfo(); // например, /36
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

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(jsonResponse);
        LOGGER.info("Отправлен HTTP " + statusCode + ": " + jsonResponse);
    }
}

@Singleton
@WebServlet("/users/*")
public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    private static final String SECRET_KEY = "SuperSecretKeyForHS256"; // Секрет для подписи JWT (храните безопасно!)

    private UserDao userDao;
    private Connection connection;
    private final Gson gson = new Gson();

    // Внедряем AccessTokenDao через DI (Guice)
    @Inject
    private AccessTokenDao accessTokenDao;

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
     * Генерирует подписанный JWT-токен с информацией о пользователе.
     * Токен действителен 7 дней.

    private String generateSignedToken(Long userId, String login) {
        Instant now = Instant.now();
        Instant expiration = now.plus(7, ChronoUnit.DAYS);
        return Jwts.builder()
                .setSubject(String.valueOf(userId))   // Идентификатор пользователя (sub)
                .claim("login", login)                // Дополнительный claim
                .setIssuedAt(Date.from(now))          // Время выпуска (iat)
                .setExpiration(Date.from(expiration)) // Время истечения (exp)
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    /**
     * Проверяет валидность JWT-токена. В случае ошибок выбрасывает исключение.

    private Claims verifyToken(String token) throws Exception {
        try {
            return Jwts.parser()
                    .setSigningKey(SECRET_KEY)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new Exception("Токен просрочен", e);
        } catch (MalformedJwtException e) {
            throw new Exception("Неверная подпись или формат токена", e);
        } catch (Exception e) {
            throw new Exception("Ошибка валидации токена", e);
        }
    }

    /**
     * PUT /users/{id}
     * Асинхронное обновление данных пользователя и продление сессии посредством генерации нового JWT.

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        LOGGER.info("Получен PUT-запрос: " + req.getRequestURI());

        // Проверяем наличие Bearer-токена в заголовке
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(resp, 401, "{\"message\": \"Access token is missing or invalid\"}");
            return;
        }
        String token = authHeader.substring(7);

        // Проверяем JWT-токен
        Claims claims;
        try {
            claims = verifyToken(token);
        } catch (Exception e) {
            sendJsonResponse(resp, 403, "{\"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        // Получаем userId из URL
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
            sendJsonResponse(resp, 400, "{\"message\": \"Invalid user_id format\"}");
            return;
        }

        // Сравниваем userId из токена с тем, что указан в URL
        if (!claims.getSubject().equals(String.valueOf(userId))) {
            sendJsonResponse(resp, 403, "{\"message\": \"Token does not belong to this user\"}");
            return;
        }

        // Читаем тело запроса (JSON)
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("Request body: " + body);

        User updatedUser;
        try {
            updatedUser = gson.fromJson(body, User.class);
            LOGGER.info("Parsed updatedUser: " + gson.toJson(updatedUser));
        } catch (Exception e) {
            LOGGER.warning("Incorrect JSON format: " + e.getMessage());
            sendJsonResponse(resp, 422, "{\"message\": \"Incorrect JSON format\"}");
            return;
        }

        try {
            // Получаем существующего пользователя из БД
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
            String newEmail = null;
            if (updatedUser.getEmails() != null && !updatedUser.getEmails().isEmpty()) {
                newEmail = updatedUser.getEmails().get(0);
                existingUser.setLogin(newEmail);
                LOGGER.info("Updated login (email) to: " + newEmail);
            }

            final long finalUserId = userId;
            final User finalExistingUser = existingUser;
            final String finalNewEmail = newEmail;

            // Асинхронное обновление данных пользователя
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

            // Асинхронное обновление данных доступа (например, изменение логина в таблице user_access)
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

            // Генерируем новый подписанный JWT-токен для продления сессии
            String newAccessToken = generateSignedToken(finalUserId, (finalNewEmail != null) ? finalNewEmail : existingUser.getLogin());
            LOGGER.info("🔑 Сгенерирован новый JWT Access Token: " + newAccessToken);

            // Получаем обновленные данные пользователя
            User refreshedUser = userDao.getUserDetailsById(finalUserId);
            LOGGER.info("Updated user (and access) details: " + gson.toJson(refreshedUser));

            JsonObject responseJson = new JsonObject();
            responseJson.addProperty("message", "Користувач успішно оновлений");
            responseJson.addProperty("token", newAccessToken);
            responseJson.add("user", gson.toJsonTree(refreshedUser));

            sendJsonResponse(resp, 200, responseJson.toString());

        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            LOGGER.log(Level.SEVERE, "[Async] Error updating user ID=" + userId, cause);
            sendJsonResponse(resp, 500, "{\"message\": \"Error during asynchronous update: " + cause.getMessage() + "\"}");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating user ID=" + userId, e);
            sendJsonResponse(resp, 500, "{\"message\": \"Error updating user: " + e.getMessage() + "\"}");
        }
    }


     * GET /users/{id}
     * Возвращает данные пользователя, если в заголовке передан корректный Bearer JWT-токен.

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupResponseHeaders(resp);
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendJsonResponse(resp, 401, "{\"message\": \"Access token is missing or invalid\"}");
            return;
        }
        String token = authHeader.substring(7);

        // Проверяем JWT-токен
        Claims claims;
        try {
            claims = verifyToken(token);
        } catch (Exception e) {
            sendJsonResponse(resp, 403, "{\"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        // Получаем userId из URL
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

        // Сравниваем userId из токена и из URL
        if (!claims.getSubject().equals(String.valueOf(userId))) {
            sendJsonResponse(resp, 403, "{\"message\": \"Token does not belong to this user\"}");
            return;
        }

        try {
            User user = userDao.getUserById(userId);
            if (user == null) {
                sendJsonResponse(resp, 404, "{\"message\": \"User not found\"}");
                return;
            }
            sendJsonResponse(resp, 200, gson.toJson(user));
        } catch (SQLException e) {
            sendJsonResponse(resp, 500, "{\"message\": \"Database error\"}");
        }
    }


     * DELETE /users/{id}
     * Мягкое удаление (soft delete) пользователя.

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

    private boolean isUserExists(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (var rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void setupResponseHeaders(HttpServletResponse resp) {
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

    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String jsonResponse) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(jsonResponse);
        LOGGER.info("Отправлен HTTP " + statusCode + ": " + jsonResponse);
    }
}*/