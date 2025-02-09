package itstep.learning.dal.dao;

import itstep.learning.models.User;

import java.sql.*;
import java.util.Base64;
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

    // Метод для создания таблицы `users`
    public boolean installUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id CHAR(36) PRIMARY KEY, " +
                "name VARCHAR(128) NOT NULL, " +
                "email VARCHAR(256), " +
                "phone VARCHAR(32)" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблица `users` успешно создана или уже существует.");
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка при создании таблицы `users`: " + e.getMessage());
            return false;
        }
    }

    // Метод для добавления пользователя в таблицу `users`
    public User addUser(User user) {
        // Генерация user_id, если оно отсутствует
        if (user.getId() == 0) {
            user.setId(Long.parseLong(UUID.randomUUID().toString()));
        }

        // SQL-запрос для добавления записи в таблицу `users`
        String userSql = "INSERT INTO users (user_id, name, email, phone) VALUES (?, ?, ?, ?)";

        try (PreparedStatement userPrep = this.connection.prepareStatement(userSql)) {
            // Установка значений для таблицы `users`
            userPrep.setString(1, String.valueOf(user.getId())); // Преобразуем long в String
            userPrep.setString(2, user.getName());
            userPrep.setString(3, user.getEmails() != null && !user.getEmails().isEmpty() ? user.getEmails().get(0) : null);
            userPrep.setString(4, user.getPhones() != null && !user.getPhones().isEmpty() ? user.getPhones().get(0) : null);
            userPrep.executeUpdate();
            logger.info("UserDao::addUser: Пользователь успешно добавлен в таблицу `users`.");

            // SQL-запрос для добавления записи в таблицу `users_access`
            String accessSql = "INSERT INTO users_access (user_access_id, user_id, role_id, login, salt, dk) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement accessPrep = this.connection.prepareStatement(accessSql)) {
                // Генерация данных для записи в `users_access`
                String userAccessId = UUID.randomUUID().toString();
                String roleId = "guest"; // Назначение роли guest
                String login = user.getLogin();
                String salt = UUID.randomUUID().toString().substring(0, 16); // Генерация соли
                String dk = Base64.getEncoder().encodeToString(login.getBytes()); // Кодирование DK

                // Установка значений для таблицы `users_access`
                accessPrep.setString(1, userAccessId);
                accessPrep.setString(2, String.valueOf(user.getId())); // Преобразуем long в String
                accessPrep.setString(3, roleId);
                accessPrep.setString(4, login);
                accessPrep.setString(5, salt);
                accessPrep.setString(6, dk);
                accessPrep.executeUpdate();

                logger.info("UserDao::addUser: Роль guest успешно добавлена в таблицу `users_access`.");
            }

            return user;
        } catch (SQLException ex) {
            logger.warning("UserDao::addUser: Ошибка при добавлении пользователя: " + ex.getMessage());
            return null;
        }
    }


    // Метод для получения текущего времени
    public String fetchCurrentTime() {
        String sql = "SELECT CURRENT_TIMESTAMP";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            if (resultSet.next()) {
                return resultSet.getString(1); // Возвращаем текущую дату/время
            }
        } catch (SQLException e) {
            logger.severe("UserDao::fetchCurrentTime: Ошибка при выполнении запроса: " + e.getMessage());
        }
        return null; // Возвращаем null, если запрос не удался
    }

    // Метод для получения списка баз данных
    public String fetchDatabases() {
        String sql = "SHOW DATABASES"; // Запрос для получения списка баз данных
        StringBuilder databases = new StringBuilder();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                if (databases.length() > 0) {
                    databases.append(", "); // Разделитель между названиями
                }
                databases.append(resultSet.getString(1)); // Добавляем название базы данных
            }
        } catch (SQLException e) {
            logger.severe("UserDao::fetchDatabases: Ошибка при выполнении запроса: " + e.getMessage());
            return null; // Возвращаем null в случае ошибки
        }

        return databases.toString(); // Возвращаем список баз данных
    }

    // Дополнительно: Создание таблицы `users_access`
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

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблица `users_access` успешно создана или уже существует.");
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка при создании таблицы `users_access`: " + e.getMessage());
            return false;
        }
    }

    // Дополнительно: Создание таблицы `user_roles`
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles (" +
                "id VARCHAR(16) PRIMARY KEY, " +
                "description VARCHAR(256) NOT NULL, " +
                "canCreate BOOLEAN NOT NULL, " +
                "canRead BOOLEAN NOT NULL, " +
                "canUpdate BOOLEAN NOT NULL, " +
                "canDelete BOOLEAN NOT NULL" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Таблица `user_roles` успешно создана или уже существует.");
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка при создании таблицы `user_roles`: " + e.getMessage());
            return false;
        }
    }
}