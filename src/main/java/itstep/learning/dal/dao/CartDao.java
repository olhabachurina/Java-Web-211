package itstep.learning.dal.dao;

import itstep.learning.dal.dto.Cart;
import itstep.learning.dal.dto.CartItem;
import itstep.learning.services.DbService.DbService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class CartDao {

    private final DbService dbService;
    private final Logger logger;

    @Inject
    public CartDao(DbService dbService, Logger logger) {
        this.dbService = dbService;
        this.logger = logger;
    }

    // ---------------------------------------------------
    // Создание таблиц
    // ---------------------------------------------------
    public boolean installTables() {
        logger.info("🔧 Установка таблицы 'carts'...");
        return installCarts();
    }

    private boolean installCarts() {
        String sql = """
            CREATE TABLE IF NOT EXISTS carts (
                cart_id         CHAR(36) PRIMARY KEY,
                user_access_id  CHAR(36) NOT NULL,
                role_id         VARCHAR(16) NOT NULL,
                login           VARCHAR(128) NOT NULL UNIQUE,
                salt            CHAR(16) NOT NULL,
                derived_key     CHAR(20) NOT NULL,
                cart_created_at DATETIME,
                cart_closed_at  DATETIME,
                is_cancelled    BOOLEAN DEFAULT FALSE,
                cart_price      DOUBLE NOT NULL
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return false;
            }
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                logger.info("✅ Таблица 'carts' успешно создана или уже существует.");
                return true;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при создании таблицы 'carts': " + e.getMessage(), e);
            return false;
        }
    }

    // ---------------------------------------------------
    // CRUD: Create
    // ---------------------------------------------------
    public boolean createCart(Cart cart) {
        String sql = """
            INSERT INTO carts (
                cart_id, user_access_id, role_id, login, salt, derived_key,
                cart_created_at, cart_closed_at, is_cancelled, cart_price
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return false;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cart.getCartId());
                stmt.setString(2, cart.getUserAccessId());
                stmt.setString(3, cart.getRoleId());
                stmt.setString(4, cart.getLogin());
                stmt.setString(5, cart.getSalt());
                stmt.setString(6, cart.getDerivedKey());
                stmt.setTimestamp(7, toTimestamp(cart.getCartCreatedAt()));
                stmt.setTimestamp(8, toTimestamp(cart.getCartClosedAt()));
                stmt.setBoolean(9, cart.isCancelled());
                stmt.setDouble(10, cart.getCartPrice());

                int rows = stmt.executeUpdate();
                logger.info("✅ Корзина добавлена. Строк добавлено: " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при добавлении корзины: " + e.getMessage(), e);
            return false;
        }
    }

    // ---------------------------------------------------
    // CRUD: Read (по cartId)
    // ---------------------------------------------------
    public Optional<Cart> getCartById(String cartId) {
        String sql = "SELECT * FROM carts WHERE cart_id = ?";

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return Optional.empty();
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cartId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Cart cart = mapResultSetToCart(rs);
                        logger.info("✅ Корзина найдена: " + cart.getCartId());
                        return Optional.of(cart);
                    } else {
                        logger.warning("⚠️ Корзина с ID " + cartId + " не найдена.");
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при получении корзины: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------
    // CRUD: Read (по user_access_id)
    // ---------------------------------------------------
    public Optional<Cart> getCartByUserAccessId(String userAccessId) {
        String sql = """
            SELECT *
            FROM carts
            WHERE user_access_id = ?
              AND cart_closed_at IS NULL
              AND is_cancelled = FALSE
            ORDER BY cart_created_at DESC
            LIMIT 1
        """;

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return Optional.empty();
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userAccessId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Cart cart = mapResultSetToCart(rs);
                        logger.info("✅ Корзина найдена по userAccessId: " + userAccessId);
                        return Optional.of(cart);
                    } else {
                        logger.warning("⚠️ Нет открытой корзины для userAccessId: " + userAccessId);
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при получении корзины по userAccessId "
                    + userAccessId + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    // ---------------------------------------------------
    // CRUD: Update
    // ---------------------------------------------------
    public boolean updateCart(Cart cart) {
        String sql = """
            UPDATE carts SET
                user_access_id = ?, role_id = ?, login = ?, salt = ?, derived_key = ?,
                cart_created_at = ?, cart_closed_at = ?, is_cancelled = ?, cart_price = ?
            WHERE cart_id = ?
        """;

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return false;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cart.getUserAccessId());
                stmt.setString(2, cart.getRoleId());
                stmt.setString(3, cart.getLogin());
                stmt.setString(4, cart.getSalt());
                stmt.setString(5, cart.getDerivedKey());
                stmt.setTimestamp(6, toTimestamp(cart.getCartCreatedAt()));
                stmt.setTimestamp(7, toTimestamp(cart.getCartClosedAt()));
                stmt.setBoolean(8, cart.isCancelled());
                stmt.setDouble(9, cart.getCartPrice());
                stmt.setString(10, cart.getCartId());

                int rows = stmt.executeUpdate();
                logger.info("✅ Корзина обновлена. Строк затронуто: " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при обновлении корзины: " + e.getMessage(), e);
            return false;
        }
    }
    public List<CartItem> getCartItemsByCartId(String cartId) {
        String sql = "SELECT * FROM cart_items WHERE cart_id = ?";
        List<CartItem> items = new ArrayList<>();

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cartId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    CartItem item = mapResultSetToCartItem(rs);
                    items.add(item);
                }
            }

            logger.info("✅ Найдено товаров в корзине: " + items.size());
            return items;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка получения товаров корзины: " + e.getMessage(), e);
            return items;
        }
    }

    private CartItem mapResultSetToCartItem(ResultSet rs) throws SQLException {
        return new CartItem(
                UUID.fromString(rs.getString("cart_item_id")),   // Primary key
                UUID.fromString(rs.getString("cart_id")),        // Связь с корзиной
                UUID.fromString(rs.getString("product_id")),     // Связь с продуктом
                null,                                            // action_id (если используешь — дополни здесь!)
                rs.getDouble("cart_item_price"),                 // Цена товара в корзине
                rs.getShort("quantity")                          // Количество товара
        );
    }

    // ✅ Добавить товар в корзину
    public boolean addCartItem(CartItem item) {
        String sql = """
        INSERT INTO cart_items (cart_item_id, cart_id, product_id, quantity, cart_item_price)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, item.getCartItemId().toString());
            stmt.setString(2, item.getCartId().toString());
            stmt.setString(3, item.getProductId().toString());
            stmt.setShort(4, item.getQuantity());
            stmt.setDouble(5, item.getCartItemPrice());

            int rows = stmt.executeUpdate();
            logger.info("✅ Добавлен товар в корзину: " + rows);
            return rows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка добавления товара в корзину: " + e.getMessage(), e);
            return false;
        }
    }

    // ---------------------------------------------------
    // CRUD: Delete
    // ---------------------------------------------------
    public boolean deleteCart(String cartId) {
        String sql = "DELETE FROM carts WHERE cart_id = ?";

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return false;
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, cartId);
                int rows = stmt.executeUpdate();
                logger.info("✅ Корзина удалена. Строк затронуто: " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при удалении корзины: " + e.getMessage(), e);
            return false;
        }
    }

    // ---------------------------------------------------
    // CRUD: Get All
    // ---------------------------------------------------
    public List<Cart> getAllCarts() {
        String sql = "SELECT * FROM carts";
        List<Cart> carts = new ArrayList<>();

        try (Connection conn = dbService.getConnection()) {
            if (!isConnectionValid(conn)) {
                return carts;
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Cart cart = mapResultSetToCart(rs);
                    carts.add(cart);
                }
            }
            logger.info("✅ Получено корзин: " + carts.size());
            return carts;
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при получении всех корзин: " + e.getMessage(), e);
            return carts;
        }
    }

    // ---------------------------------------------------
    // Вспомогательные методы
    // ---------------------------------------------------
    private Cart mapResultSetToCart(ResultSet rs) throws SQLException {
        return new Cart(
                rs.getString("cart_id"),
                rs.getString("user_access_id"),
                rs.getString("role_id"),
                rs.getString("login"),
                rs.getString("salt"),
                rs.getString("derived_key"),
                toLocalDateTime(rs.getTimestamp("cart_created_at")),
                toLocalDateTime(rs.getTimestamp("cart_closed_at")),
                rs.getBoolean("is_cancelled"),
                rs.getDouble("cart_price")
        );
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        return dateTime != null ? Timestamp.valueOf(dateTime) : null;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private boolean isConnectionValid(Connection connection) {
        try {
            if (connection == null || connection.isClosed()) {
                logger.severe("❌ Соединение с базой данных отсутствует или закрыто!");
                return false;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при проверке соединения: " + e.getMessage(), e);
            return false;
        }
        return true;
    }
}
