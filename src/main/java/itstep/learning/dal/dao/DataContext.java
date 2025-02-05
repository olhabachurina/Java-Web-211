package itstep.learning.dal.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.services.DbService.DbService;

import java.sql.Connection;
import java.util.logging.Logger;

@Singleton
public class DataContext {
    private final Connection connection;
    private final Logger logger;
    private final UserDao userDao;

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
}