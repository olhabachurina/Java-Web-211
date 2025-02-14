package itstep.learning.dal.dao;

import itstep.learning.models.User;

import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class UserDao {
    private final Connection connection;
    private final Logger logger;

    public UserDao(Connection connection, Logger logger) {
        this.connection = connection;
        this.logger = (logger != null) ? logger : Logger.getLogger(UserDao.class.getName());
    }

    /**
     * ✅ Добавление пользователя + запись в `users_access`
     */
    public void addUser(User user) throws SQLException {
        String userSql = "INSERT INTO users (name, login, city, address, birthdate, password) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String accessSql = "INSERT INTO users_access (user_access_id, user_id, role_id, login, salt, dk) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        if (connection == null) {
            throw new SQLException("❌ Ошибка: Нет подключения к базе данных.");
        }

        connection.setAutoCommit(false);
        try (PreparedStatement userStmt = connection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement accessStmt = connection.prepareStatement(accessSql)) {

            // 🟢 Вставляем пользователя в `users`
            userStmt.setString(1, user.getName());
            userStmt.setString(2, user.getLogin());
            userStmt.setString(3, user.getCity());
            userStmt.setString(4, user.getAddress());
            userStmt.setString(5, user.getBirthdate());
            userStmt.setString(6, user.getPassword());
            userStmt.executeUpdate();

            // ✅ Получаем сгенерированный `user_id`
            ResultSet generatedKeys = userStmt.getGeneratedKeys();
            if (!generatedKeys.next()) {
                throw new SQLException("❌ Ошибка: Не удалось получить `user_id`.");
            }
            long userId = generatedKeys.getLong(1);
            user.setId(userId);

            // ✅ Генерируем данные для `users_access`
            String userAccessId = UUID.randomUUID().toString();
            String salt = UUID.randomUUID().toString().substring(0, 16);
            String dk = Base64.getEncoder().encodeToString(user.getLogin().getBytes());

            // 🟢 Вставляем данные в `users_access`
            accessStmt.setString(1, userAccessId);
            accessStmt.setLong(2, userId);
            accessStmt.setString(3, "USER");
            accessStmt.setString(4, user.getLogin());
            accessStmt.setString(5, salt);
            accessStmt.setString(6, dk);
            accessStmt.executeUpdate();

            connection.commit();
            logger.info("✅ Пользователь добавлен в `users` и `users_access`.");
        } catch (SQLException ex) {
            connection.rollback();
            logger.severe("❌ Ошибка при добавлении пользователя: " + ex.getMessage());
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * ✅ Получение всех пользователей + email'ы + телефоны
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.id, u.name, u.login, e.email " +
                "FROM users u " +
                "LEFT JOIN user_emails e ON u.id = e.user_id";

        if (connection == null) {
            logger.severe("❌ Ошибка: Нет подключения к базе данных.");
            return users;
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            Map<Long, User> userMap = new HashMap<>();

            while (rs.next()) {
                long userId = rs.getLong("id"); //

                // Используем map, чтобы не дублировать пользователей
                User user = userMap.computeIfAbsent(userId, key -> {
                    try {
                        return new User(key, rs.getString("name"), rs.getString("login")); //
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });


                String email = rs.getString("email");
                if (email != null) {
                    user.getEmails().add(email);
                }
            }

            users.addAll(userMap.values());
        } catch (SQLException ex) {
            logger.severe("❌ Ошибка при получении списка пользователей: " + ex.getMessage());
            throw new RuntimeException("Ошибка базы данных", ex);
        }

        return users;
    }

    /**
     * ✅ Создание таблицы `users`
     */
    public boolean installUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(128) NOT NULL, " +
                "login VARCHAR(128) NOT NULL UNIQUE, " +
                "city VARCHAR(128), " +
                "address VARCHAR(256), " +
                "birthdate DATE, " +
                "password VARCHAR(256) NOT NULL, " +
                "registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ Таблица `users` создана.");
    }

    /**
     * ✅ Создание таблицы `users_access`
     */
    public boolean installUserAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access (" +
                "user_access_id CHAR(36) PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "role_id VARCHAR(16) NOT NULL, " +
                "login VARCHAR(128) NOT NULL UNIQUE, " +
                "salt CHAR(16) NOT NULL, " +
                "dk CHAR(20) NOT NULL, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ Таблица `users_access` создана.");
    }

    /**
     * ✅ Создание таблицы `user_roles`
     */
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles (" +
                "id VARCHAR(16) PRIMARY KEY, " +
                "description VARCHAR(256) NOT NULL " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ Таблица `user_roles` создана.");
    }

    /**
     * ✅ Выполнение SQL-запросов
     */
    private boolean executeStatement(String sql, String successMessage) {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info(successMessage);
            return true;
        } catch (SQLException e) {
            logger.severe("❌ Ошибка выполнения SQL: " + e.getMessage());
            return false;
        }
    }

    public String fetchCurrentTime() {
        return fetchSingleValue("SELECT CURRENT_TIMESTAMP", "❌ Ошибка при получении текущего времени");
    }
    private String fetchSingleValue(String sql, String errorMessage) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next() ? resultSet.getString(1) : null;
        } catch (SQLException e) {
            logger.severe(errorMessage + ": " + e.getMessage());
            return null;
        }
    }

    public String fetchDatabases() {
        String sql = "SHOW DATABASES";
        StringBuilder databases = new StringBuilder();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                if (databases.length() > 0) databases.append(", ");
                databases.append(resultSet.getString(1));
            }
            return databases.toString();
        } catch (SQLException e) {
            logger.severe("❌ Ошибка при получении списка баз данных: " + e.getMessage());
            return null;
        }
    }
}