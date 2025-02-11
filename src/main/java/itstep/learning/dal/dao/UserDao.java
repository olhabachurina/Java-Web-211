package itstep.learning.dal.dao;

import itstep.learning.models.User;

import java.sql.*;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UserDao {
    private final Connection connection;
    private final Logger logger;

    public UserDao(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = logger;
    }

    // Создание таблицы `users`
    public boolean installUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id CHAR(36) PRIMARY KEY, " +
                "name VARCHAR(128) NOT NULL, " +
                "email VARCHAR(256), " +
                "phone VARCHAR(32)" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "Таблица `users` успешно создана.");
    }

    // Создание таблицы `users_access`
    public boolean installUserAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access (" +
                "user_access_id CHAR(36) PRIMARY KEY, " +
                "user_id CHAR(36) NOT NULL, " +
                "role_id VARCHAR(16) NOT NULL, " +
                "login VARCHAR(128) NOT NULL UNIQUE, " +
                "salt CHAR(16) NOT NULL, " +
                "dk CHAR(20) NOT NULL, " +
                "UNIQUE(user_id, role_id)" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "Таблица `users_access` успешно создана.");
    }

    // Создание таблицы `user_roles`
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles (" +
                "id VARCHAR(16) PRIMARY KEY, " +
                "description VARCHAR(256) NOT NULL, " +
                "canCreate BOOLEAN NOT NULL, " +
                "canRead BOOLEAN NOT NULL, " +
                "canUpdate BOOLEAN NOT NULL, " +
                "canDelete BOOLEAN NOT NULL" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "Таблица `user_roles` успешно создана.");
    }

    // Добавление пользователя и связанных данных
    public User addUser(User user) {
        // Проверяем и генерируем ID пользователя, если отсутствует
        if (user.getId() == 0) {
            user.setId(Long.parseLong(UUID.randomUUID().toString().replaceAll("-", "").substring(0, 15)));
        }

        String userSql = "INSERT INTO users (name, login, city, address, birthdate, password) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement userPrep = this.connection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
            // Добавление записи в таблицу `users`
            userPrep.setString(1, user.getName());
            userPrep.setString(2, user.getLogin());
            userPrep.setString(3, user.getCity());
            userPrep.setString(4, user.getAddress());
            userPrep.setString(5, user.getBirthdate());
            userPrep.setString(6, user.getPassword());
            userPrep.executeUpdate();

            // Получение сгенерированного user_id
            ResultSet keys = userPrep.getGeneratedKeys();
            if (keys.next()) {
                user.setId(keys.getLong(1));
            } else {
                throw new SQLException("Не удалось получить ID пользователя");
            }

            // Добавление записи в таблицу `users_access`
            String accessSql = "INSERT INTO users_access (user_access_id, user_id, role_id, login, salt, dk) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement accessPrep = this.connection.prepareStatement(accessSql)) {
                // Генерация уникальных значений
                String userAccessId = UUID.randomUUID().toString();
                String roleId = "viewer"; // Назначаем роль по умолчанию
                String salt = UUID.randomUUID().toString().substring(0, 16);
                String dk = Base64.getEncoder().encodeToString(user.getLogin().getBytes());

                // Устанавливаем параметры
                accessPrep.setString(1, userAccessId);
                accessPrep.setLong(2, user.getId());
                accessPrep.setString(3, roleId);
                accessPrep.setString(4, user.getLogin());
                accessPrep.setString(5, salt);
                accessPrep.setString(6, dk);
                accessPrep.executeUpdate();
            }

            return user;
        } catch (SQLException ex) {
            logger.warning("Ошибка при добавлении пользователя: " + ex.getMessage());
            return null;
        }
    }

    // Получение текущего времени
    public String fetchCurrentTime() {
        return fetchSingleValue("SELECT CURRENT_TIMESTAMP", "Ошибка при получении времени");
    }

    // Получение списка баз данных
    public String fetchDatabases() {
        String sql = "SHOW DATABASES";
        StringBuilder databases = new StringBuilder();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                if (databases.length() > 0) {
                    databases.append(", ");
                }
                databases.append(resultSet.getString(1));
            }
        } catch (SQLException e) {
            logger.severe("Ошибка при получении списка баз данных: " + e.getMessage());
        }

        return databases.toString();
    }

    // Вспомогательный метод: выполнение SQL-запросов на создание таблиц
    private boolean executeStatement(String sql, String successMessage) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info(successMessage);
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка выполнения SQL: " + e.getMessage());
            return false;
        }
    }

    // Вспомогательный метод: выполнение запросов, возвращающих одно значение
    private String fetchSingleValue(String sql, String errorMessage) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            logger.severe(errorMessage + ": " + e.getMessage());
        }
        return null;
    }

    // Вспомогательный метод: получение первого элемента из списка или null
    private String getFirstOrNull(List<String> list) {
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}