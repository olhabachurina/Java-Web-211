package itstep.learning.dal.dao;


import itstep.learning.dal.dto.Category;
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
public class CategoryDao {
    private final DbService dbService;
    private final Logger logger;

    @Inject
    public CategoryDao(DbService dbService, Logger logger) {
        this.dbService = dbService;
        this.logger = logger;
    }

    // ✅ Отримання всіх категорій
    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories";

        logger.info("📥 Отримання всіх категорій...");

        try (Connection connection = dbService.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            logger.info("📄 Виконання SQL запиту:\n" + sql);

            while (rs.next()) {
                Category category = Category.fromResultSet(rs);
                categories.add(category);
                logger.info("✅ Категорія знайдена: " + category.getCategoryTitle() + " | ID: " + category.getCategoryId());
            }

            logger.info("✅ Загалом отримано " + categories.size() + " категорій.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні категорій: " + e.getMessage(), e);
        }

        return categories;
    }

    // ✅ Отримання категорії за ID
    public Category getCategoryById(UUID categoryId) {
        String sql = "SELECT * FROM categories WHERE category_id = ?";

        logger.info("📥 Пошук категорії за ID: " + categoryId);

        try (Connection connection = dbService.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, categoryId.toString());
            logger.info("📄 Виконання SQL запиту:\n" + sql);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.info("✅ Категорія знайдена: " + categoryId);
                    return Category.fromResultSet(rs);
                } else {
                    logger.warning("⚠️ Категорія з ID " + categoryId + " не знайдена.");
                }
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при отриманні категорії за ID: " + e.getMessage(), e);
        }

        return null;
    }

    // ✅ Створення таблиці категорій
    public boolean installTables() {
        String sql = "CREATE TABLE IF NOT EXISTS categories (" +
                "category_id CHAR(36) PRIMARY KEY, " +
                "category_slug VARCHAR(64) NOT NULL UNIQUE, " +
                "category_title VARCHAR(64) NOT NULL, " +
                "category_description VARCHAR(256) NOT NULL, " +
                "category_image_id VARCHAR(64) NOT NULL, " +
                "category_delete_moment DATETIME NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

        logger.info("🔧 Початок створення таблиці 'categories'...");

        try (Connection connection = dbService.getConnection();
             Statement statement = connection.createStatement()) {

            logger.info("📄 SQL для створення таблиці:\n" + sql);
            statement.executeUpdate(sql);

            logger.info("✅ Таблиця 'categories' успішно створена або вже існує.");

            return seedData();

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Помилка при створенні таблиці 'categories': " + e.getMessage(), e);
            return false;
        }
    }

    // ✅ Заповнення таблиці тестовими категоріями
    public boolean seedData() {
        String checkSql = "SELECT COUNT(*) FROM categories";
        String sql = "INSERT IGNORE INTO categories (category_id, category_slug, category_title, category_description, category_image_id, category_delete_moment) VALUES "
                + "('14780dcf-fb75-11ef-90a1-62517600596c', 'glass', 'Вироби зі скла', 'Декоративні вироби зі скла', 'glass.jpg', NULL), "
                + "('24780dcf-fb75-11ef-90a1-62517600596c', 'office', 'Офісні товари', 'Настільні сувеніри', 'office.jpg', NULL), "
                + "('34780dcf-fb75-11ef-90a1-62517600596c', 'stone', 'Вироби з каменю', 'Декоративні вироби з каменю', 'stone.jpg', NULL), "
                + "('44780dcf-fb75-11ef-90a1-62517600596c', 'wood', 'Вироби з дерева', 'Декоративні вироби з дерева', 'wood.jpg', NULL);";

        logger.info("🔍 Перевірка наявності даних у таблиці 'categories'...");

        try (Connection connection = dbService.getConnection()) {

            if (connection == null || connection.isClosed()) {
                logger.severe("❌ З'єднання з базою даних відсутнє або закрите!");
                return false;
            }

            try (Statement checkStatement = connection.createStatement();
                 ResultSet resultSet = checkStatement.executeQuery(checkSql)) {

                resultSet.next();
                int rowCount = resultSet.getInt(1);

                if (rowCount > 0) {
                    logger.warning("⚠️ Дані вже існують у 'categories', вставка пропущена.");
                    return false;
                }
            }

            logger.info("📄 Виконання SQL для вставки початкових категорій:\n" + sql);

            try (Statement statement = connection.createStatement()) {
                int rowsInserted = statement.executeUpdate(sql);

                logger.info("✅ Успішно вставлено " + rowsInserted + " категорій.");

                connection.commit();
                logger.info("✅ Коміт успішно виконано.");
                return true;
            }

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "❌ Помилка при вставці тестових даних у 'categories': " + ex.getMessage(), ex);
            return false;
        }
    }
}
