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
        // Если logger == null, используем логгер по умолчанию для UserDao
        this.logger = (logger != null) ? logger : Logger.getLogger(UserDao.class.getName());
    }

    /**
     * ✅ Добавление пользователя (users), создание записи в users_access,
     *    а также сохранение email'ов (user_emails) и телефонов (user_phones).
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
            // Если ошибка — делаем rollback
            logger.severe("❌ [UserDao.addUser] Ошибка при добавлении пользователя: " + ex.getMessage());
            connection.rollback();
            throw ex;
        } finally {
            // Возвращаем auto-commit в true (исходное состояние)
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
     * ✅ Получение всех пользователей + их e‑mail’ы + телефоны
     *    Через двойной LEFT JOIN.
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();

        // Двойной LEFT JOIN:
        // e.email, p.phone могут быть null, если нет записей
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

            // Вспомогательная карта, чтобы собирать User без дублирования
            Map<Long, User> userMap = new HashMap<>();

            while (rs.next()) {
                long userId = rs.getLong("id");

                // Если в map ещё нет пользователя с таким userId, создаём
                User user = userMap.get(userId);
                if (user == null) {
                    user = new User();
                    user.setId(userId);
                    user.setName(rs.getString("name"));
                    user.setLogin(rs.getString("login"));
                    user.setCity(rs.getString("city"));
                    user.setAddress(rs.getString("address"));
                    user.setBirthdate(rs.getString("birthdate"));

                    // Инициализируем пустые списки для e‑mail и phones
                    user.setEmails(new ArrayList<>());
                    user.setPhones(new ArrayList<>());

                    userMap.put(userId, user);
                }

                // Добавляем e‑mail (если не null)
                String email = rs.getString("email");
                if (email != null) {
                    user.getEmails().add(email);
                }

                // Добавляем телефон (если не null)
                String phone = rs.getString("phone");
                if (phone != null) {
                    user.getPhones().add(phone);
                }
            }

            // Формируем итоговый список
            users.addAll(userMap.values());

        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "❌ [UserDao.getAllUsers] Ошибка при получении списка пользователей: ", ex);
            throw new RuntimeException("Ошибка базы данных", ex);
        }

        logger.info("✅ [UserDao.getAllUsers] Всего пользователей: " + users.size());
        return users;
    }

    /**
     * ✅  users
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
                " registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ [UserDao.installUsers] Таблица users создана/проверена.");
    }

    /**
     * users_access
     */
    public boolean installUserAccess() {
        String sql = "CREATE TABLE IF NOT EXISTS users_access (" +
                " user_access_id CHAR(36) PRIMARY KEY, " +
                " user_id BIGINT NOT NULL, " +
                " role_id VARCHAR(16) NOT NULL, " +
                " login VARCHAR(128) NOT NULL UNIQUE, " +
                " salt CHAR(16) NOT NULL, " +
                " dk CHAR(20) NOT NULL, " +
                " FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ [UserDao.installUserAccess] Таблица users_access создана/проверена.");
    }

    /**
     *  user_roles
     */
    public boolean installUserRoles() {
        String sql = "CREATE TABLE IF NOT EXISTS user_roles (" +
                " id VARCHAR(16) PRIMARY KEY, " +
                " description VARCHAR(256) NOT NULL " +
                ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        return executeStatement(sql, "✅ [UserDao.installUserRoles] Таблица user_roles создана/проверена.");
    }

    /**
     * user_emails
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
     * ✅  user_phones
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
        if (!isUserExists(user.getId())) {
            logger.warning("⚠ Користувача з id=" + user.getId() + " не знайдено.");
            return;
        }

        String sql = "UPDATE users SET name = ?, city = ?, address = ?, birthdate = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getCity());
            stmt.setString(3, user.getAddress());
            stmt.setString(4, user.getBirthdate());
            stmt.setLong(5, user.getId());
            stmt.executeUpdate();
        }

        updateEmails(user.getId(), user.getEmails());
        updatePhones(user.getId(), user.getPhones());

        logger.info("✅ Користувач оновлений: " + user.getLogin());
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

    public void deleteUser(Long userId) throws SQLException {
        if (!isUserExists(userId)) {
            logger.warning("⚠ Користувач з id=" + userId + " не знайдений.");
            return;
        }

        deleteEmails(userId);
        deletePhones(userId);

        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
        logger.info("✅ Користувач з id=" + userId + " успішно видалений.");
    }
    /**
     * Удаление Email'ов пользователя
     */
    private void deleteEmails(Long userId) throws SQLException {
        String sql = "DELETE FROM user_emails WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
        logger.info("   -> [deleteEmails] user_id=" + userId + " удалено.");
    }
    private void updateEmails(Long userId, List<String> emails) throws SQLException {
        deleteEmails(userId);
        saveEmails(userId, emails);
    }

    private void updatePhones(Long userId, List<String> phones) throws SQLException {
        deletePhones(userId);
        savePhones(userId, phones);
    }
    /**
     * Удаление Телефонов пользователя
     */
    private void deletePhones(Long userId) throws SQLException {
        String sql = "DELETE FROM user_phones WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
        logger.info("   -> [deletePhones] user_id=" + userId + " удалено.");
    }
    private List<String> parseList(String data) {
        return (data != null && !data.isEmpty()) ? Arrays.asList(data.split("; ")) : new ArrayList<>();
    }

    public User getUserById(long userId) throws SQLException {
        String sql = "SELECT id, name, login, city, address, birthdate FROM users WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("login"),
                            rs.getString("city"),
                            rs.getString("address"),
                            rs.getString("birthdate"),
                            "USER" // Роль по умолчанию
                    );
                }
            }
        }
        return null; // Если пользователь не найден
    }
}