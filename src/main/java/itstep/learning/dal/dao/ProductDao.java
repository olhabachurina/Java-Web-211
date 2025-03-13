package itstep.learning.dal.dao;

import itstep.learning.dal.dto.Category;
import itstep.learning.dal.dto.Product;
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

        logger.info("🔧 Створення таблиці 'products'...");

        try (Connection connection = dbService.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate(sql);
            logger.info("✅ Таблиця 'products' створена або вже існує");
            return true;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при створенні таблиці 'products': " + e.getMessage(), e);
            return false;
        }
    }
    // ========================
    // ===== UPDATE PRODUCT ===
    // ========================
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET " +
                "name = ?, " +
                "description = ?, " +
                "price = ?, " +
                "code = ?, " +
                "stock = ?, " +
                "category_id = ?, " +
                "image_id = ? " +
                "WHERE product_id = ?";

        logger.info("✏️ Оновлення продукту з ID: " + product.getProductId());
        logger.info("📦 Дані продукту для оновлення: " + product);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            if (connection == null || connection.isClosed()) {
                logger.severe("❌ Підключення до БД відсутнє або закрите!");
                return false;
            }

            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setDouble(3, product.getPrice());
            stmt.setString(4, product.getCode());
            stmt.setInt(5, product.getStock());
            stmt.setString(6, product.getCategoryId().toString());
            stmt.setString(7, product.getImageId());
            stmt.setString(8, product.getProductId().toString());

            logger.info("📄 Виконання SQL оновлення продукту:\n" + sql);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("✅ Продукт оновлено успішно: " + product.getProductId());
                return true;
            } else {
                logger.warning("⚠️ Продукт не оновлено (ID не знайдено?): " + product.getProductId());
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при оновленні продукту: " + e.getMessage(), e);
            return false;
        }
    }

    // ==============================
    // ===== PAGED PRODUCT SEARCH ===
    // ==============================
    public List<Product> getProductsByCategoryPaged(UUID categoryId, int limit, int offset) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE category_id = ? LIMIT ? OFFSET ?";

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, categoryId.toString());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);

            logger.info("📄 SQL для пагінованого вибору по категорії:\n" + sql);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(Product.fromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні продуктів по категорії: " + e.getMessage(), e);
        }

        return products;
    }
    public boolean addProduct(Product product) {
        String sql = "INSERT INTO products (product_id, name, description, price, code, stock, category_id, image_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        logger.info("📥 Додавання продукту: " + product.getName());

        // Перевірки
        if (product.getCategoryId() == null) {
            logger.warning("⚠️ [addProduct] categoryId is NULL! Запит не виконується.");
            return false;
        }
        if (product.getProductId() == null) {
            logger.warning("⚠️ [addProduct] productId is NULL! Запит не виконується.");
            return false;
        }

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            // Логування параметрів
            logger.info("➡️ productId: " + product.getProductId());
            logger.info("➡️ name: " + product.getName());
            logger.info("➡️ description: " + product.getDescription());
            logger.info("➡️ price: " + product.getPrice());
            logger.info("➡️ code: " + product.getCode());
            logger.info("➡️ stock: " + product.getStock());
            logger.info("➡️ categoryId: " + product.getCategoryId());
            logger.info("➡️ imageId: " + product.getImageId());

            stmt.setString(1, product.getProductId().toString());
            stmt.setString(2, product.getName());
            stmt.setString(3, product.getDescription());
            stmt.setDouble(4, product.getPrice());
            stmt.setString(5, product.getCode());
            stmt.setInt(6, product.getStock());
            stmt.setString(7, product.getCategoryId().toString());
            stmt.setString(8, product.getImageId() != null ? product.getImageId() : "");

            int rowsAffected = stmt.executeUpdate();
            logger.info("✅ Записано рядків: " + rowsAffected);

            return rowsAffected > 0;

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ SQL: " + sql);
            logger.log(Level.SEVERE, "❌ Параметри: " + product.toString());
            logger.log(Level.SEVERE, "❌ Помилка при додаванні продукту: " + e.getMessage(), e);
            return false;
        }
    }
    // ========================
    // ===== EXISTS BY CODE ===
    // ========================
    public boolean existsByCode(String code) {
        String sql = "SELECT 1 FROM products WHERE code = ?";

        logger.info("🔍 Перевірка існування продукту з кодом: " + code);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, code);

            logger.info("📄 Виконання SQL exists by code:\n" + sql);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next();
                if (exists) {
                    logger.info("⚠️ Продукт із кодом '" + code + "' існує");
                } else {
                    logger.info("✅ Код '" + code + "' вільний");
                }
                return exists;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при перевірці коду '" + code + "': " + e.getMessage(), e);
            return false;
        }
    }

    // ========================
    // ===== GET PRODUCT BY ID ===
    // ========================
    public Product getProductById(UUID productId) {
        String sql = "SELECT * FROM products WHERE product_id = ?";

        logger.info("🔍 Пошук продукту за ID: " + productId);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, productId.toString());

            logger.info("📄 Виконання SQL get by ID:\n" + sql);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Product product = Product.fromResultSet(rs);
                    logger.info("✅ Продукт знайдено: " + product);
                    return product;
                } else {
                    logger.warning("⚠️ Продукт з ID " + productId + " не знайдено.");
                    return null;
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні продукту за ID: " + e.getMessage(), e);
            return null;
        }
    }

    // ========================
    // ===== DELETE PRODUCT ===
    // ========================
    public boolean deleteProductById(UUID productId) {
        String sql = "DELETE FROM products WHERE product_id = ?";

        logger.info("🗑️ Видалення продукту з ID: " + productId);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, productId.toString());

            logger.info("📄 Виконання SQL delete:\n" + sql);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("✅ Продукт видалено: " + productId);
                return true;
            } else {
                logger.warning("⚠️ Продукт не знайдено для видалення: " + productId);
                return false;
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при видаленні продукту: " + e.getMessage(), e);
            return false;
        }
    }
    public List<Product> getProductsPaged(int limit, int offset, String search) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE 1=1 ";

        if (search != null && !search.isEmpty()) {
            sql += "AND (name LIKE ? OR description LIKE ?) ";
        }

        sql += "ORDER BY name ASC LIMIT ? OFFSET ?";

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            int paramIndex = 1;

            if (search != null && !search.isEmpty()) {
                stmt.setString(paramIndex++, "%" + search + "%");
                stmt.setString(paramIndex++, "%" + search + "%");
            }

            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            logger.info("📄 SQL запит до products з пошуком або пагінацією: " + sql);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(Product.fromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні продуктів (paged): " + e.getMessage(), e);
        }

        return products;
    }
    public Category getCategoryBySlug(String slug) {
        String sql = "SELECT * FROM categories WHERE category_slug = ?";
        logger.info("🔍 Виконання пошуку категорії по slug: " + slug);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, slug);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("✅ Категорія знайдена по slug: " + slug);
                    return Category.fromResultSet(rs);
                } else {
                    logger.warning("⚠️ Категорія зі slug " + slug + " не знайдена.");
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при пошуку категорії по slug: " + e.getMessage(), e);
        }

        return null;
    }

    // ========================
    // ===== GET ALL PRODUCTS ===
    // ========================
    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products ORDER BY name ASC";

        logger.info("📥 Отримання всіх продуктів...");

        try (Connection connection = dbService.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            logger.info("📄 Виконання SQL get all:\n" + sql);

            while (rs.next()) {
                Product product = Product.fromResultSet(rs);
                products.add(product);
                logger.info("✅ Завантажено продукт: " + product);
            }

            logger.info("✅ Кількість продуктів: " + products.size());

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні всіх продуктів: " + e.getMessage(), e);
        }

        return products;
    }
}