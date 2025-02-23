package itstep.learning.dal.dao;

import itstep.learning.models.User;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;




public class UserDao {
    private final Connection connection;
    private final Logger logger;

    public UserDao(Connection connection, Logger logger) {
        this.connection = connection;
        // Если logger == null, используем логгер по умолчанию для UserDao
        this.logger = (logger != null) ? logger : Logger.getLogger(UserDao.class.getName());
    }

    /**
     * ✅ Добавление пользователя (users), создание записи в users_access,
     * а также сохранение email'ов (user_emails) и телефонов (user_phones).
     */
    public void addUser(User user) throws SQLException {
        // 1) Проверяем, есть ли уже пользователь с таким логином
        if (isLoginExists(user.getLogin())) {
            throw new SQLException("❌ Логин уже используется: " + user.getLogin());
        }

        // 2) Подготавливаем SQL
        String userSql = "INSERT INTO users (name, login, city, address, birthdate, password) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        String accessSql = "INSERT INTO users_access (user_access_id, user_id, role_id, login, salt, dk) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        logger.info("🔎 [UserDao.addUser] Начинаем транзакцию для добавления пользователя: " + user);
        // Отключаем auto-commit (создаём транзакцию)
        connection.setAutoCommit(false);

        try (PreparedStatement userStmt = connection.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement accessStmt = connection.prepareStatement(accessSql)) {

            // 🔎 LOG:
            logger.info("   -> INSERT INTO users: name=" + user.getName()
                    + ", login=" + user.getLogin()
                    + ", city=" + user.getCity()
                    + ", address=" + user.getAddress()
                    + ", birthdate=" + user.getBirthdate());

            // 3) Добавление в таблицу `users`
            userStmt.setString(1, user.getName());
            userStmt.setString(2, user.getLogin());
            userStmt.setString(3, user.getCity());
            userStmt.setString(4, user.getAddress());
            userStmt.setString(5, user.getBirthdate());
            userStmt.setString(6, user.getPassword());
            userStmt.executeUpdate();

            // 4) Получаем сгенерированный user_id
            try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                if (!generatedKeys.next()) {
                    throw new SQLException("❌ Ошибка: Не удалось получить user_id из users.");
                }
                long userId = generatedKeys.getLong(1);
                logger.info("   -> Сгенерированный user_id=" + userId);

                // 5) Добавление в таблицу `users_access`
                //    Для демонстрации role_id='USER', salt=случайная строка, dk=Base64(login)
                logger.info("   -> INSERT INTO users_access: user_id=" + userId + ", role=USER, login=" + user.getLogin());
                accessStmt.setString(1, UUID.randomUUID().toString()); // user_access_id
                accessStmt.setLong(2, userId);
                accessStmt.setString(3, "USER"); // роль
                accessStmt.setString(4, user.getLogin());
                accessStmt.setString(5, UUID.randomUUID().toString().substring(0, 16)); // salt
                accessStmt.setString(6, Base64.getEncoder().encodeToString(user.getLogin().getBytes())); // dk
                accessStmt.executeUpdate();

                // 6) Сохраняем e‑mails (если они есть)
                saveEmails(userId, user.getEmails());
                // 7) Сохраняем телефоны (если они есть)
                savePhones(userId, user.getPhones());
                // 8) Коммитим транзакцию
                connection.commit();
                logger.info("✅ [UserDao.addUser] Пользователь добавлен успешно (user_id=" + userId + ") вместе с emails/phones.");
            }

        } catch (SQLException ex) {
            logger.severe("❌ [UserDao.addUser] Ошибка при добавлении пользователя: " + ex.getMessage());
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * ✅ Сохранение e‑mail’ов в таблице user_emails
     */
    private void saveEmails(long userId, List<String> emails) throws SQLException {
        if (emails == null || emails.isEmpty()) {
            logger.info("   -> [saveEmails] Список emails пуст, ничего не сохраняем.");
            return;
        }
        String emailSql = "INSERT INTO user_emails (user_id, email) VALUES (?, ?)";
        logger.info("   -> [saveEmails] user_id=" + userId + ", emails=" + emails);

        try (PreparedStatement emailStmt = connection.prepareStatement(emailSql)) {
            for (String email : emails) {
                logger.info("      INSERT email: " + email);
                emailStmt.setLong(1, userId);
                emailStmt.setString(2, email);
                emailStmt.executeUpdate();
            }
        }
    }

    /**
     * ✅ Сохранение телефонов в таблице user_phones
     */
    private void savePhones(long userId, List<String> phones) throws SQLException {
        if (phones == null || phones.isEmpty()) {
            logger.info("   -> [savePhones] Список phones пуст, ничего не сохраняем.");
            return;
        }
        String phoneSql = "INSERT INTO user_phones (user_id, phone) VALUES (?, ?)";
        logger.info("   -> [savePhones] user_id=" + userId + ", phones=" + phones);

        try (PreparedStatement phoneStmt = connection.prepareStatement(phoneSql)) {
            for (String phone : phones) {
                logger.info("      INSERT phone: " + phone);
                phoneStmt.setLong(1, userId);
                phoneStmt.setString(2, phone);
                phoneStmt.executeUpdate();
            }
        }
    }

    /**
     * ✅ Проверка существования логина (по таблице users_access)
     */
    private boolean isLoginExists(String login) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users_access WHERE login = ?";
        logger.info("🔎 [UserDao.isLoginExists] Проверяем логин: " + login);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, login);
            try (ResultSet rs = stmt.executeQuery()) {
                boolean exists = rs.next() && rs.getInt(1) > 0;
                logger.info("   -> Результат: " + exists);
                return exists;
            }
        }
    }

    /**
     * ✅ Получение всех пользователей + их e‑mail’ы + телефоны через двойной LEFT JOIN.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.id, u.name, u.login, u.city, u.address, u.birthdate, " +
                "       e.email, p.phone " +
                "FROM users u " +
                "LEFT JOIN user_emails e ON u.id = e.user_id " +
                "LEFT JOIN user_phones p ON u.id = p.user_id";

        if (connection == null) {
            logger.severe("❌ [UserDao.getAllUsers] Нет подключения к базе данных.");
            return users;
        }
        logger.info("🔎 [UserDao.getAllUsers] Запрашиваем всех пользователей с e-mails и phones.");

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {

            Map<Long, User> userMap = new HashMap<>();
            while (rs.next()) {
                long userId = rs.getLong("id");
                User user = userMap.get(userId);
                if (user == null) {
                    user = new User();
                    user.setId(userId);
                    user.setName(rs.getString("name"));
                    user.setLogin(rs.getString("login"));
                    user.setCity(rs.getString("city"));
                    user.setAddress(rs.getString("address"));
                    user.setBirthdate(rs.getString("birthdate"));
                    user.setEmails(new ArrayList<>());
                    user.setPhones(new ArrayList<>());
                    userMap.put(userId, user);
                }
                String email = rs.getString("email");
                if (email != null && !user.getEmails().contains(email)) {
                    user.getEmails().add(email);
                }
                String phone = rs.getString("phone");
                if (phone != null && !user.getPhones().contains(phone)) {
                    user.getPhones().add(phone);
                }
            }
            users.addAll(userMap.values());
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "❌ [UserDao.getAllUsers] Ошибка при получении списка пользователей: ", ex);
            throw new RuntimeException("Ошибка базы данных", ex);
        }
        logger.info("✅ [UserDao.getAllUsers] Всего пользователей: " + users.size());
        return users;
    }

    public void updateUserPhones(long userId, List<String> phones) throws SQLException {
        try (PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM user_phones WHERE user_id = ?");
             PreparedStatement insertStmt = connection.prepareStatement("INSERT INTO user_phones (user_id, phone) VALUES (?, ?)")) {
            deleteStmt.setLong(1, userId);
            deleteStmt.executeUpdate();
            for (String phone : phones) {
                insertStmt.setLong(1, userId);
                insertStmt.setString(2, phone);
                insertStmt.executeUpdate();
            }
        }
    }

    /**
     * ✅ Таблица users
     */
    public boolean installUsers() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                " id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                " name VARCHAR(128) NOT NULL, " +
                " login VARCHAR(128) NOT NULL UNIQUE, " +
                " city VARCHAR(128), " +
                " address VARCHAR(256), " +
                " birthdate DATE, " +
                " password VARCHAR(256) NOT NULL, " +
                " registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                " delete_moment TIMESTAMP NULL" +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";
        return executeStatement(sql, "✅ [UserDao.installUsers] Таблица users создана/проверена.");
    }

    /**
     * ✅ Таблица users_access
     */
    public boolean installUserAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access (" +
                " user_access_id CHAR(36) PRIMARY KEY, " +
                " user_id BIGINT NOT NULL, " +
                " role_id VARCHAR(16) NOT NULL, " +
                " login VARCHAR(128) NOT NULL UNIQUE, " +
                " salt CHAR(16) NOT NULL, " +
                " dk CHAR(20) NOT NULL, " +
                " is_deleted BOOLEAN DEFAULT false, " +
                " delete_moment TIMESTAMP NULL, " +
                " FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";
        return executeStatement(sql, "✅ [UserDao.installUserAccess] Таблица users_access создана/проверена.");
    }

    public void softDeleteUserAccess(String userAccessId) throws SQLException {
        if (userAccessId == null || userAccessId.trim().isEmpty()) {
            logger.warning("⚠ Недопустимый userAccessId: " + userAccessId);
            throw new IllegalArgumentException("userAccessId не должен быть пустым");
        }
        String sql = "UPDATE users_access SET " +
                "is_deleted = ?, " +
                "delete_moment = ? " +
                "WHERE user_access_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setString(3, userAccessId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("✅ Запись в users_access помечена как удалённая (user_access_id=" + userAccessId + ")");
            } else {
                logger.warning("⚠ Запись в users_access не найдена или не изменена (user_access_id=" + userAccessId + ")");
            }
        }
    }

    /**
     * Асинхронное мягкое удаление пользователя (soft delete)
     */
    public void softDeleteUser(Long userId) throws SQLException {
        if (!isUserExists(userId)) {
            logger.warning("⚠ Користувача з ID=" + userId + " не знайдено.");
            return;
        }

        Timestamp deleteMoment = new Timestamp(System.currentTimeMillis());
        // Генеруємо унікальний логін, наприклад "deleted_37" для userId=37
        String uniqueLogin = "deleted_" + userId;

        String sql = "UPDATE users SET " +
                "name = ?, " +
                "login = ?, " +
                "city = ?, " +
                "address = ?, " +
                "birthdate = ?, " +
                "password = ?, " +
                "delete_moment = ? " +
                "WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "Deleted User");      // анонімізоване ім'я
            stmt.setString(2, uniqueLogin);           // унікальний логін
            stmt.setString(3, "");                    // очищення міста
            stmt.setString(4, "");                    // очищення адреси
            stmt.setDate(5, java.sql.Date.valueOf("1900-01-01")); // заглушка для birthdate
            stmt.setString(6, "");                    // очищення пароля
            stmt.setTimestamp(7, deleteMoment);       // фіксація моменту видалення
            stmt.setLong(8, userId);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                logger.info("✅ Користувач з ID=" + userId + " успішно анонімізований та позначений як видалений. Момент видалення: " + deleteMoment);
            } else {
                logger.warning("⚠ Не вдалося анонімізувати користувача з ID=" + userId);
            }
        }
    }

    /**
     * Таблица user_roles
     */
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles (" +
                " id VARCHAR(16) PRIMARY KEY, " +
                " description VARCHAR(256) NOT NULL " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";
        return executeStatement(sql, "✅ [UserDao.installUserRoles] Таблица user_roles создана/проверена.");
    }

    /**
     * Таблица user_emails
     */
    public boolean installUserEmails() {
        String sql = "CREATE TABLE IF NOT EXISTS user_emails (" +
                " id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                " user_id BIGINT NOT NULL, " +
                " email VARCHAR(255) NOT NULL, " +
                " FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";
        return executeStatement(sql, "✅ [UserDao.installUserEmails] Таблица user_emails создана/проверена.");
    }

    /**
     * Таблица user_phones
     */
    public boolean installUserPhones() {
        String sql = "CREATE TABLE IF NOT EXISTS user_phones (" +
                " id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                " user_id BIGINT NOT NULL, " +
                " phone VARCHAR(50) NOT NULL, " +
                " FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";
        return executeStatement(sql, "✅ [UserDao.installUserPhones] Таблица user_phones создана/проверена.");
    }

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

    public void updateUser(User user) throws SQLException {
        String sql = "UPDATE users SET name = ?, login = ?, city = ?, address = ?, birthdate = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getCity());
            stmt.setString(4, user.getAddress());
            if (user.getBirthdate() == null || user.getBirthdate().isEmpty()) {
                stmt.setNull(5, Types.DATE);
            } else {
                stmt.setDate(5, java.sql.Date.valueOf(user.getBirthdate()));
            }
            stmt.setLong(6, user.getId());
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("❌ Ошибка: Пользователь не обновлен, возможно, не найден!");
            }
        }
    }

    private boolean isUserExists(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void updateUserAccessLogin(long userId, String newLogin) throws SQLException {
        String sql = "UPDATE users_access SET login = ? WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newLogin);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    public CompletableFuture<Void> updateUserAsync(User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateUser(user);
                logger.info("✅ [Async] updateUser выполнен для пользователя ID=" + user.getId());
            } catch (SQLException e) {
                logger.severe("❌ [Async] Ошибка при updateUser: " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Асинхронное обновление данных доступа (таблица users_access), обновляем login.
     */
    public CompletableFuture<Void> updateUserAccessLoginAsync(long userId, String newLogin) {
        return CompletableFuture.runAsync(() -> {
            try {
                updateUserAccessLogin(userId, newLogin);
                logger.info("✅ [Async] updateUserAccessLogin выполнен для пользователя ID=" + userId + " с новым login=" + newLogin);
            } catch (SQLException e) {
                logger.severe("❌ [Async] Ошибка при updateUserAccessLogin: " + e.getMessage());
                throw new CompletionException(e);
            }
        });
    }

    public User getUserDetailsById(long userId) throws SQLException {
        String sql = "SELECT u.id, u.name, u.login, u.city, u.address, u.birthdate, " +
                "       e.email, p.phone " +
                "FROM users u " +
                "LEFT JOIN user_emails e ON u.id = e.user_id " +
                "LEFT JOIN user_phones p ON u.id = p.user_id " +
                "WHERE u.id = ?";
        User user = null;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (user == null) {
                        user = new User();
                        user.setId(rs.getLong("id"));
                        user.setName(rs.getString("name"));
                        user.setLogin(rs.getString("login"));
                        user.setCity(rs.getString("city"));
                        user.setAddress(rs.getString("address"));
                        user.setBirthdate(rs.getString("birthdate"));
                        user.setEmails(new ArrayList<>());
                        user.setPhones(new ArrayList<>());
                    }
                    String email = rs.getString("email");
                    if (email != null && !user.getEmails().contains(email)) {
                        user.getEmails().add(email);
                    }
                    String phone = rs.getString("phone");
                    if (phone != null && !user.getPhones().contains(phone)) {
                        user.getPhones().add(phone);
                    }
                }
            }
        }
        return user;
    }

    public User getUserById(long userId) throws SQLException {
        String query = "SELECT id, name, login FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getLong("id"), rs.getString("name"), rs.getString("login"));
                }
            }
        }
        return null;
    }
}