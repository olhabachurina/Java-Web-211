package itstep.learning.dal.dao;

import itstep.learning.dal.dto.CartItem;
import itstep.learning.models.Order;
import itstep.learning.services.DbService.DbService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class OrdersDao {
    private final DbService dbService;
    private final Logger logger;

    @Inject
    public OrdersDao(DbService dbService, Logger logger) {
        this.dbService = dbService;
        this.logger = logger;
    }

    // ✅ Установка таблиц
    public boolean installTables() {
        boolean ordersTableCreated = createOrdersTable();   // СНАЧАЛА создаем orders
        boolean orderItemsTableCreated = createOrderItemsTable(); // потом order_items

        if (ordersTableCreated && orderItemsTableCreated) {
            logger.info("✅ Таблицы orders и order_items успешно созданы или уже существуют.");
            return true;
        } else {
            logger.warning("⚠️ Были ошибки при создании таблиц orders и/или order_items.");
            return false;
        }
    }

    // ✅ Создание таблицы orders
    private boolean createOrdersTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS orders (
            order_id CHAR(36) PRIMARY KEY,
            user_id CHAR(36) NOT NULL,
            total_price DOUBLE NOT NULL,
            status VARCHAR(20) DEFAULT 'NEW',
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    """;

        try (Connection connection = dbService.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.executeUpdate(sql);
            logger.info("✅ Таблица 'orders' успешно создана или уже существует.");
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при создании таблицы 'orders': " + e.getMessage(), e);
            return false;
        }
    }

    // ✅ Создание таблицы order_items
    private boolean createOrderItemsTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS order_items (
            order_item_id CHAR(36) PRIMARY KEY,
            order_id CHAR(36) NOT NULL,
            product_id CHAR(36) NOT NULL,
            quantity SMALLINT NOT NULL,
            price DOUBLE NOT NULL,
            INDEX idx_order_id (order_id),
            INDEX idx_product_id (product_id),
            FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE,
            FOREIGN KEY (product_id) REFERENCES products(product_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
    """;


        try (Connection connection = dbService.getConnection();
             Statement stmt = connection.createStatement()) {

            stmt.executeUpdate(sql);
            logger.info("✅ Таблица 'order_items' успешно создана или уже существует.");
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при создании таблицы 'order_items': " + e.getMessage(), e);
            return false;
        }
    }

    // ✅ Добавление заказа
    public boolean createOrder(Order order) {
        String sql = """
            INSERT INTO orders (order_id, user_id, total_price, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, order.getOrderId().toString());
            stmt.setString(2, order.getUserId().toString());
            stmt.setDouble(3, order.getTotalPrice());
            stmt.setString(4, order.getStatus());
            stmt.setTimestamp(5, Timestamp.valueOf(order.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(order.getUpdatedAt()));

            int rows = stmt.executeUpdate();

            logger.info("✅ Заказ добавлен. Строк затронуто: " + rows);

            // После вставки заказа — добавляем товары
            saveOrderItems(order, connection);

            return rows > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка добавления заказа: " + e.getMessage(), e);
            return false;
        }
    }

    // ✅ Получить список заказов пользователя
    public List<Order> getOrdersByUserId(UUID userId) {
        String sql = "SELECT * FROM orders WHERE user_id = ?";
        List<Order> orders = new ArrayList<>();

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, userId.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = mapResultSetToOrder(rs);
                order.setItems(getOrderItems(order.getOrderId(), connection)); // подтягиваем товары
                orders.add(order);
            }

            logger.info("✅ Заказы пользователя получены: " + orders.size());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка получения заказов пользователя: " + e.getMessage(), e);
        }

        return orders;
    }

    // ✅ Приватные методы
    private Order mapResultSetToOrder(ResultSet rs) throws SQLException {
        return new Order(
                UUID.fromString(rs.getString("order_id")),
                UUID.fromString(rs.getString("user_id")),
                rs.getDouble("total_price"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                new ArrayList<>() // items подтянем потом
        );
    }

    private void saveOrderItems(Order order, Connection connection) throws SQLException {
        String sql = """
            INSERT INTO order_items (order_item_id, order_id, product_id, quantity, price)
            VALUES (?, ?, ?, ?, ?)
        """;

        for (CartItem item : order.getItems()) {
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, UUID.randomUUID().toString());
                stmt.setString(2, order.getOrderId().toString());
                stmt.setString(3, item.getProductId().toString());
                stmt.setInt(4, item.getQuantity());
                stmt.setDouble(5, item.getCartItemPrice());

                stmt.executeUpdate();
                logger.info("✅ Товар добавлен в заказ: " + item.getProductId());
            }
        }
    }

    private List<CartItem> getOrderItems(UUID orderId, Connection connection) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ?";
        List<CartItem> items = new ArrayList<>();

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, orderId.toString());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                CartItem item = new CartItem(
                        UUID.fromString(rs.getString("order_item_id")),
                        UUID.fromString(rs.getString("order_id")),
                        UUID.fromString(rs.getString("product_id")),
                        null, // actionId если есть
                        rs.getDouble("price"),
                        rs.getShort("quantity")
                );
                items.add(item);
            }

            logger.info("✅ Товары заказа получены: " + items.size());
        }

        return items;
    }

    public List<Order> getAllOrders() {
        String sql = "SELECT * FROM orders";
        List<Order> orders = new ArrayList<>();

        try (Connection connection = dbService.getConnection()) {
            if (!isConnectionValid(connection)) {
                return orders;
            }
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Order order = mapResultSetToOrder(rs);
                    // Если необходимо подтягивать товары заказа, можно добавить:
                    // order.setItems(getOrderItems(order.getOrderId(), connection));
                    orders.add(order);
                }
            }
            logger.info("✅ Получено заказов: " + orders.size());
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при получении всех заказов: " + e.getMessage(), e);
        }
        return orders;
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

    public boolean updateOrder(Order order) {
        String sql = """
        UPDATE orders SET
            user_id = ?,
            total_price = ?,
            status = ?,
            updated_at = ?
        WHERE order_id = ?
    """;

        try (Connection connection = dbService.getConnection()) {
            if (!isConnectionValid(connection)) {
                return false;
            }
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, order.getUserId().toString());
                stmt.setDouble(2, order.getTotalPrice());
                stmt.setString(3, order.getStatus());
                stmt.setTimestamp(4, Timestamp.valueOf(order.getUpdatedAt()));
                stmt.setString(5, order.getOrderId().toString());

                int rows = stmt.executeUpdate();
                logger.info("✅ Заказ обновлен. Строк затронуто: " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при обновлении заказа: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteOrder(String orderId) {
        String sql = "DELETE FROM orders WHERE order_id = ?";

        try (Connection connection = dbService.getConnection()) {
            if (!isConnectionValid(connection)) {
                return false;
            }
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, orderId);
                int rows = stmt.executeUpdate();
                logger.info("✅ Заказ удален. Строк затронуто: " + rows);
                return rows > 0;
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка при удалении заказа: " + e.getMessage(), e);
            return false;
        }
    }
}
