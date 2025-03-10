package itstep.learning.dal.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dto.Category;
import itstep.learning.services.DbService.DbService;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
public class DataContext {
    private final Connection connection;
    private final Logger logger;
    private final UserDao userDao;
    private AccessTokenDao accessTokenDao;

    @Inject
    public DataContext(DbService dbService, Logger logger) {
        this.connection = dbService.getConnection(); // Отримання з'єднання з DbService
        this.logger = logger; // Логгер для відстеження подій
        this.userDao = new UserDao(connection, logger); // Передаємо логгер до UserDao
        logger.info("DataContext успішно ініціалізований.");
    }

    public UserDao getUserDao() {
        return userDao;
    }
    public AccessTokenDao getAccessTokenDao() {
        return this.accessTokenDao;
    }
    public boolean installTables() {
        try {
            logger.info("Початок встановлення таблиць...");
            boolean usersInstalled = userDao.installUsers(); // Встановлення таблиці `users`
            boolean accessInstalled = userDao.installUserAccess(); // Встановлення таблиці `users_access`
            boolean rolesInstalled = userDao.installUserRoles(); // Встановлення таблиці `user_roles`

            if (usersInstalled && accessInstalled && rolesInstalled) {
                logger.info("Усі таблиці успішно створено.");
                return true;
            } else {
                logger.warning("Деякі таблиці не вдалося створити.");
                return false;
            }
        } catch (Exception e) {
            logger.severe("Помилка під час встановлення таблиць: " + e.getMessage());
            return false;
        }
    }
    public void initializeRolesAndAccess() {
        logger.info("Инициализация ролей и данных доступа...");
        try {
            // Создание таблиц, если их нет
            userDao.installUsers();
            userDao.installUserAccess();
            userDao.installUserRoles();

            // Инициализация ролей
            initializeDefaultRoles();
            logger.info("Инициализация ролей завершена успешно.");
        } catch (Exception e) {
            logger.severe("Ошибка при инициализации ролей: " + e.getMessage());
        }
    }
    public boolean initializeDefaultRoles() {
        String sql = "INSERT IGNORE INTO user_roles (id, description, canCreate, canRead, canUpdate, canDelete) VALUES " +
                "('admin', 'Administrator', true, true, true, true), " +
                "('editor', 'Editor', false, true, true, false), " +
                "('viewer', 'Viewer', false, true, false, false)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            logger.info("Роли успешно добавлены или уже существуют.");
            return true;
        } catch (SQLException e) {
            logger.severe("Ошибка при добавлении ролей: " + e.getMessage());
            return false;
        }
    }
}