package itstep.learning.dal.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UserDao {
    private final Connection connection;
    private final Logger logger;

    public UserDao(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    // Створення таблиці `users`
    public boolean installUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "user_id CHAR(36) PRIMARY KEY," // Унікальний ідентифікатор користувача
                + "name VARCHAR(128) NOT NULL," // Ім'я користувача
                + "email VARCHAR(256)," // Електронна пошта користувача
                + "phone VARCHAR(32)" // Телефон користувача
                + ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблиця `users` успішно створена або вже існує.");
            return true;
        } catch (SQLException e) {
            logger.severe("Помилка при створенні таблиці `users`: " + e.getMessage());
            return false;
        }
    }

    // Створення таблиці `users_access`
    public boolean installUserAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access ("
                + "user_access_id CHAR(36) PRIMARY KEY," // Унікальний ідентифікатор доступу
                + "user_id CHAR(36) NOT NULL," // Ідентифікатор користувача
                + "role_id VARCHAR(16) NOT NULL," // Ідентифікатор ролі
                + "login VARCHAR(128) NOT NULL UNIQUE," // Логін користувача
                + "salt CHAR(16) NOT NULL," // Сіль для хешування пароля
                + "dk CHAR(20) NOT NULL," // Деривативний ключ
                + "UNIQUE(user_id, role_id)" // Унікальне обмеження для комбінації user_id і role_id
                + ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблиця `users_access` успішно створена або вже існує.");
            return true;
        } catch (SQLException e) {
            logger.severe("Помилка при створенні таблиці `users_access`: " + e.getMessage());
            return false;
        }
    }

    // Створення таблиці `user_roles`
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles ("
                + "id VARCHAR(16) PRIMARY KEY," // Унікальний ідентифікатор ролі
                + "description VARCHAR(256) NOT NULL," // Опис ролі
                + "canCreate BOOLEAN NOT NULL," // Дозвіл на створення
                + "canRead BOOLEAN NOT NULL," // Дозвіл на читання
                + "canUpdate BOOLEAN NOT NULL," // Дозвіл на оновлення
                + "canDelete BOOLEAN NOT NULL" // Дозвіл на видалення
                + ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблиця `user_roles` успішно створена або вже існує.");
            return true;
        } catch (SQLException e) {
            logger.severe("Помилка при створенні таблиці `user_roles`: " + e.getMessage());
            return false;
        }
    }

    // Отримання поточного часу з бази даних
    public String fetchCurrentTime() {
        String sql = "SELECT CURRENT_TIMESTAMP";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getString(1); // Повертаємо першу колонку результату
            }
        } catch (SQLException e) {
            logger.severe("UserDao::fetchCurrentTime: Помилка при виконанні запиту: " + e.getMessage());
        }
        return null; // Повертаємо null, якщо запит не вдалося виконати
    }

    // Отримання списку баз даних
    public String fetchDatabases() {
        String sql = "SHOW DATABASES"; // Запит для отримання списку баз даних
        StringBuilder databases = new StringBuilder();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                if (databases.length() > 0) {
                    databases.append(", "); // Роздільник між назвами баз даних
                }
                databases.append(resultSet.getString(1)); // Додаємо назву бази даних
            }
        } catch (SQLException e) {
            logger.severe("UserDao::fetchDatabases: Помилка при виконанні запиту: " + e.getMessage());
            return null; // Повертаємо null у разі помилки
        }

        return databases.toString(); // Повертаємо список баз даних через кому
    }
}