package itstep.learning.dal.dao;

import itstep.learning.dal.dto.Product;
import itstep.learning.services.DbService.DbService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class ProductDao {
    private final DbService dbService;
    private final Logger logger;

    @Inject
    public ProductDao(DbService dbService, Logger logger) {
        this.dbService = dbService;
        this.logger = logger;
    }

    public boolean installTables() {
        String sql = "CREATE TABLE IF NOT EXISTS products (" +
                "product_id CHAR(36) PRIMARY KEY, " +
                "name VARCHAR(128) NOT NULL, " +
                "description TEXT, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "code VARCHAR(32) UNIQUE NOT NULL, " +
                "stock INT NOT NULL, " +
                "category_id CHAR(36) NOT NULL, " +
                "image_id VARCHAR(64), " +
                "FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        logger.info("🔧 Початок створення таблиці 'products'...");

        try (Connection connection = dbService.getConnection();
             Statement statement = connection.createStatement()) {

            logger.info("📄 SQL для створення таблиці:\n" + sql);
            statement.executeUpdate(sql);

            logger.info("✅ Таблиця 'products' успішно створена або вже існує.");
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при створенні таблиці 'products': " + e.getMessage(), e);
            return false;
        }
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";

        logger.info("📥 Отримання всіх продуктів...");

        try (Connection connection = dbService.getConnection();
             Statement stmt = connection.createStatement()) {

            logger.info("📄 Виконання SQL запиту:\n" + sql);

            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Product product = Product.fromResultSet(rs);
                    products.add(product);
                    logger.info("✅ Завантажено продукт: " + product);
                }
            }

            logger.info("✅ Загалом отримано " + products.size() + " продуктів.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні продуктів: " + e.getMessage(), e);
        }

        return products;
    }

    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products (product_id, name, description, price, code, stock, category_id, image_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        logger.info("📥 Додавання нового продукту: " + product.getName());
        logger.info("📦 Дані продукту для вставки: " + product);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            if (connection == null || connection.isClosed()) {
                logger.severe("❌ З'єднання з БД не встановлено або закрите!");
                return false;
            }

            stmt.setString(1, product.getProductId().toString());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());
            stmt.setDouble(4, product.getPrice());
            stmt.setString(5, product.getCode());
            stmt.setInt(6, product.getStock());
            stmt.setString(7, product.getCategoryId().toString());
            stmt.setString(8, product.getImageId());

            logger.info("📄 Виконання SQL вставки продукту:\n" + sql);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("✅ Продукт '" + product.getName() + "' успішно доданий до бази даних.");
                return true;
            } else {
                logger.warning("⚠️ Продукт '" + product.getName() + "' не було додано до бази.");
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при додаванні продукту '" + product.getName() + "': " + e.getMessage(), e);
            return false;
        }
    }
}