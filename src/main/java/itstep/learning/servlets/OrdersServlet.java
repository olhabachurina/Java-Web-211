package itstep.learning.servlets;

import com.google.gson.*;
import com.mysql.cj.x.protobuf.MysqlxCrud;
import io.jsonwebtoken.Claims;
import itstep.learning.dal.dao.OrdersDao;
import itstep.learning.models.Order;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.DbService.MySqlDbService;
import itstep.learning.services.JwtService;
import itstep.learning.services.LocalDateTimeAdapter;
import itstep.learning.services.config.ConfigService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebServlet("/orders/*")
@Singleton
public class OrdersServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(OrdersServlet.class.getName());

    @Inject
    private OrdersDao ordersDao;

    @Inject
    private JwtUtil jwtUtil;

    private final Gson gson;

    @Inject
    public OrdersServlet(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOGGER.info("✅ [OrdersServlet] Инициализация сервлета OrdersServlet начата");

        if (ordersDao == null) {
            LOGGER.severe("❌ [OrdersServlet] ordersDao не проинициализирован!");
        } else {
            LOGGER.info("✅ [OrdersServlet] ordersDao успешно проинициализирован");
        }

        if (jwtUtil == null) {
            LOGGER.severe("❌ [OrdersServlet] jwtUtil не проинициализирован!");
        } else {
            LOGGER.info("✅ [OrdersServlet] jwtUtil успешно проинициализирован");
        }

        LOGGER.info("✅ [OrdersServlet] Инициализация сервлета OrdersServlet завершена");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupCors(resp);
        LOGGER.info("➡️ [POST] Запрос на создание заказа");

        String authHeader = req.getHeader("Authorization");
        LOGGER.info("🔐 [POST] Authorization header: " + authHeader);

        if (!isTokenValid(authHeader)) {
            LOGGER.warning("⛔ [POST] Access token is missing or invalid");
            sendJson(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }

        String token = extractBearerToken(authHeader);
        Claims claims = jwtUtil.getPayload(token);

        if (claims == null) {
            LOGGER.warning("⛔️ Невалидный или истёкший токен: " + token);
            sendJson(resp, 403, Map.of("error", "Invalid or expired token"));
            return;
        }

        // Парсим subject в JSON
        String subjectJson = claims.getSubject();
        LOGGER.info("📜 Subject JSON: " + subjectJson);

        JsonObject subject;
        try {
            subject = JsonParser.parseString(subjectJson).getAsJsonObject();
        } catch (Exception e) {
            LOGGER.warning("❌ Ошибка парсинга subject JSON: " + e.getMessage());
            sendJson(resp, 400, Map.of("error", "Invalid token payload"));
            return;
        }

        // Получаем user_id как UUID
        String userIdStr = subject.get("user_id").getAsString();
        UUID userId;
        try {
            userId = UUID.nameUUIDFromBytes(userIdStr.getBytes());  // UUID из числового user_id
            LOGGER.info("✅ userId из subject сгенерирован: " + userId);
        } catch (Exception e) {
            LOGGER.warning("❌ Невалидный user_id UUID: " + e.getMessage());
            sendJson(resp, 400, Map.of("error", "Invalid user ID format"));
            return;
        }

        // Читаем тело запроса
        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("📥 Получено тело запроса: " + body);

        try {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())  // твой адаптер
                    .create();

            Order order = gson.fromJson(body, Order.class);

            if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
                LOGGER.warning("❗ Неверные данные заказа или пустой список товаров");
                sendJson(resp, 400, Map.of("error", "Invalid order data"));
                return;
            }

            order.setUserId(userId);

            boolean created = ordersDao.createOrder(order);
            if (!created) {
                LOGGER.severe("❌ Не удалось создать заказ!");
                sendJson(resp, 500, Map.of("error", "Failed to create order"));
                return;
            }

            LOGGER.info("✅ Заказ успешно создан! orderId = " + order.getOrderId());

            sendJson(resp, 201, Map.of(
                    "message", "Заказ принят! Ждите посылку!!!",
                    "orderId", order.getOrderId()
            ));

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Ошибка при создании заказа", e);
            sendJson(resp, 500, Map.of("error", "Server error"));
        }
    }

    private boolean isTokenValid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warning("⚠️ Нет заголовка Authorization или он некорректный");
            return false;
        }

        String token = extractBearerToken(authHeader);
        boolean isValid = jwtUtil.validateToken(token);

        if (!isValid) {
            LOGGER.warning("⚠️ Токен не прошел валидацию: " + token);
        } else {
            LOGGER.info("✅ Токен валиден");
        }

        return isValid;
    }

    private String extractBearerToken(String authHeader) {
        LOGGER.info("🔍 Authorization Header: " + authHeader);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            LOGGER.info("✅ Токен получен: " + token);
            return token;
        }
        LOGGER.warning("⚠️ Токен отсутствует в заголовках!");
        return null;
    }

    private void setupCors(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Max-Age", "3600");
        LOGGER.fine("ℹ️ Заголовки CORS установлены");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        setupCors(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
        LOGGER.info("✅ [OPTIONS] Запрос успешно обработан");
    }

    private void sendJson(HttpServletResponse resp, int statusCode, Object body) throws IOException {
        String json = gson.toJson(body);

        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(json);
        }

        LOGGER.info("📤 Ответ HTTP " + statusCode + ": " + json);
    }
}