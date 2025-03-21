package itstep.learning.servlets;

import com.google.gson.Gson;
import itstep.learning.dal.dao.CartDao;
import itstep.learning.dal.dto.Cart;
import itstep.learning.dal.dto.CartItem;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

@WebServlet("/carts/*")
@Singleton
public class CartServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(CartServlet.class.getName());

    @Inject
    private CartDao cartDao;

    @Inject
    private JwtUtil jwtUtil;

    private final Gson gson = new Gson();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        LOGGER.info("✅ [CartServlet] Инициализация сервлета CartServlet начата");

        if (cartDao == null) {
            LOGGER.severe("❌ [CartServlet] cartDao не проинициализирован!");
        } else {
            LOGGER.info("✅ [CartServlet] cartDao успешно проинициализирован");
        }

        if (jwtUtil == null) {
            LOGGER.severe("❌ [CartServlet] jwtUtil не проинициализирован!");
        } else {
            LOGGER.info("✅ [CartServlet] jwtUtil успешно проинициализирован");
        }

        LOGGER.info("✅ [CartServlet] Инициализация сервлета CartServlet завершена");
    }

    // ✅ Получение корзины пользователя (GET /carts/{userId})
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupCors(resp);
        LOGGER.info("➡️ [GET] Запрос на получение корзины");

        String authHeader = req.getHeader("Authorization");
        LOGGER.info("🔐 [GET] Authorization header: " + authHeader);

        if (!isTokenValid(authHeader)) {
            LOGGER.warning("⛔ [GET] Токен отсутствует или невалиден");
            sendJson(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }

        String pathInfo = req.getPathInfo(); // /{userId}
        LOGGER.info("🔎 [GET] pathInfo: " + pathInfo);

        if (pathInfo == null || pathInfo.equals("/")) {
            LOGGER.warning("⚠️ [GET] User ID is missing in URL");
            sendJson(resp, 400, Map.of("error", "User ID is missing in URL"));
            return;
        }

        String userId = pathInfo.substring(1);
        LOGGER.info("ℹ️ [GET] Получаем корзину для userId: " + userId);

        Optional<Cart> cartOpt = cartDao.getCartByUserAccessId(userId);
        if (cartOpt.isEmpty()) {
            LOGGER.warning("⚠️ [GET] Cart not found for userId: " + userId);
            sendJson(resp, 404, Map.of("error", "Cart not found for userId: " + userId));
            return;
        }

        Cart cart = cartOpt.get();
        LOGGER.info("✅ [GET] Корзина найдена: " + cart.getCartId());

        List<CartItem> items = cartDao.getCartItemsByCartId(cart.getCartId());
        LOGGER.info("✅ [GET] Получено товаров в корзине: " + items.size());

        Map<String, Object> response = new HashMap<>();
        response.put("cart", cart);
        response.put("items", items);

        sendJson(resp, 200, response);
    }

    // ✅ Создать корзину (POST /carts)
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupCors(resp);
        LOGGER.info("➡️ [POST] Запрос на создание корзины");

        String authHeader = req.getHeader("Authorization");
        LOGGER.info("🔐 [POST] Authorization header: " + authHeader);

        if (!isTokenValid(authHeader)) {
            LOGGER.warning("⛔ [POST] Access token is missing or invalid");
            sendJson(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("📥 [POST] Получено тело запроса: " + body);

        Cart cart = gson.fromJson(body, Cart.class);

        String cartId = UUID.randomUUID().toString();
        cart.setCartId(cartId);
        LOGGER.info("🔨 [POST] Назначен cartId: " + cartId);

        boolean created = cartDao.createCart(cart);

        if (!created) {
            LOGGER.severe("❌ [POST] Failed to create cart for user: " + cart.getUserAccessId());
            sendJson(resp, 500, Map.of("error", "Failed to create cart"));
            return;
        }

        LOGGER.info("✅ [POST] Корзина успешно создана: " + cartId);
        sendJson(resp, 201, Map.of("message", "Cart created", "cartId", cartId));
    }

    // ✅ Обновить корзину (PUT /carts/{cartId})
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupCors(resp);
        LOGGER.info("➡️ [PUT] Запрос на обновление корзины");

        String authHeader = req.getHeader("Authorization");
        LOGGER.info("🔐 [PUT] Authorization header: " + authHeader);

        if (!isTokenValid(authHeader)) {
            LOGGER.warning("⛔ [PUT] Access token is missing or invalid");
            sendJson(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }

        String pathInfo = req.getPathInfo();
        LOGGER.info("🔎 [PUT] pathInfo: " + pathInfo);

        if (pathInfo == null || pathInfo.equals("/")) {
            LOGGER.warning("⚠️ [PUT] Cart ID is missing in URL");
            sendJson(resp, 400, Map.of("error", "Cart ID is missing in URL"));
            return;
        }

        String cartId = pathInfo.substring(1);
        LOGGER.info("ℹ️ [PUT] Обновляем корзину с cartId: " + cartId);

        String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        LOGGER.info("📥 [PUT] Получено тело запроса: " + body);

        Cart cart = gson.fromJson(body, Cart.class);
        cart.setCartId(cartId);

        boolean updated = cartDao.updateCart(cart);

        if (!updated) {
            LOGGER.severe("❌ [PUT] Failed to update cart: " + cartId);
            sendJson(resp, 500, Map.of("error", "Failed to update cart"));
            return;
        }

        LOGGER.info("✅ [PUT] Корзина успешно обновлена: " + cartId);
        sendJson(resp, 200, Map.of("message", "Cart updated", "cartId", cartId));
    }

    // ✅ Удалить корзину (DELETE /carts/{cartId})
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setupCors(resp);
        LOGGER.info("➡️ [DELETE] Запрос на удаление корзины");

        String authHeader = req.getHeader("Authorization");
        LOGGER.info("🔐 [DELETE] Authorization header: " + authHeader);

        if (!isTokenValid(authHeader)) {
            LOGGER.warning("⛔ [DELETE] Access token is missing or invalid");
            sendJson(resp, 401, Map.of("error", "Access token is missing or invalid"));
            return;
        }

        String pathInfo = req.getPathInfo();
        LOGGER.info("🔎 [DELETE] pathInfo: " + pathInfo);

        if (pathInfo == null || pathInfo.equals("/")) {
            LOGGER.warning("⚠️ [DELETE] Cart ID is missing in URL");
            sendJson(resp, 400, Map.of("error", "Cart ID is missing in URL"));
            return;
        }

        String cartId = pathInfo.substring(1);
        LOGGER.info("ℹ️ [DELETE] Удаляем корзину с cartId: " + cartId);

        boolean deleted = cartDao.deleteCart(cartId);

        if (!deleted) {
            LOGGER.severe("❌ [DELETE] Failed to delete cart: " + cartId);
            sendJson(resp, 500, Map.of("error", "Failed to delete cart"));
            return;
        }

        LOGGER.info("✅ [DELETE] Корзина успешно удалена: " + cartId);
        sendJson(resp, 200, Map.of("message", "Cart deleted", "cartId", cartId));
    }

    // ✅ Проверка токена (JWT)
    private boolean isTokenValid(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warning("⚠️ Нет заголовка Authorization или он некорректный");
            return false;
        }

        String token = authHeader.substring(7);
        boolean isValid = JwtUtil.validateToken(token);

        if (!isValid) {
            LOGGER.warning("⚠️ Токен не прошел валидацию: " + token);
        } else {
            LOGGER.info("✅ Токен валиден");
        }

        return isValid;
    }

    // ✅ Настройка CORS и заголовков
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

    // ✅ Отправка JSON-ответа
    private void sendJson(HttpServletResponse resp, int status, Object body) throws IOException {
        String json = gson.toJson(body);

        resp.setStatus(status);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = resp.getWriter()) {
            writer.write(json);
        }

        LOGGER.info("📤 Ответ HTTP " + status + ": " + json);
    }
}