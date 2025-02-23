package itstep.learning.dal.dao;


import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;


import com.google.inject.Singleton;
import itstep.learning.services.DbService.DbService;

@Singleton
public class AccessTokenDao {

    // Единственный экземпляр класса (Singleton)
    private static AccessTokenDao instance;

    // Логгер для данного класса
    private static final Logger logger = Logger.getLogger(AccessTokenDao.class.getName());
    private static DbService DBService;

    // Подключение к базе данных, получаемое через DBService
    private final Connection connection;

    /**
     * Приватный конструктор (паттерн Singleton).
     * При создании выводим в лог информацию о подключении.
     */
    private AccessTokenDao(Connection connection) {
        this.connection = connection;
        logger.info("AccessTokenDao: конструктор вызван. Получено подключение: " + connection);
    }

    /**
     * Метод для получения единственного экземпляра AccessTokenDao.
     * Если экземпляр ещё не создан, создаётся новый с использованием подключения из DBService.
     */
    public static synchronized AccessTokenDao getInstance() throws SQLException {
        if (instance == null) {
            logger.info("AccessTokenDao.getInstance() -> Экземпляр не найден, создаём новый через DBService...");

            Connection conn = DBService.getConnection(); // Получаем подключение из DBService
            instance = new AccessTokenDao(conn);
        } else {
            logger.info("AccessTokenDao.getInstance() -> Возвращаем существующий экземпляр.");
        }
        return instance;
    }

    /**
     * Создание таблицы access_tokens, если она не существует.
     * Добавлено подробное логирование: перед выполнением запроса и при его завершении.
     */
    public boolean installTables() {
        String sql = "CREATE TABLE IF NOT EXISTS access_tokens ("
                + " access_token_id CHAR(36) PRIMARY KEY, "
                + " user_access_id CHAR(36) NOT NULL, "
                + " issued_at DATETIME NOT NULL, "
                + " expires_at DATETIME NOT NULL"
                + ") Engine=InnoDB DEFAULT CHARSET=utf8mb4";

        logger.info("installTables: подготавливаем запрос для создания таблицы access_tokens:\n" + sql);

        try (Connection conn = DBService.getConnection();
             Statement statement = conn.createStatement()) {

            logger.info("installTables: выполняем SQL-запрос...");
            statement.executeUpdate(sql);
            logger.info("✅ [AccessTokenDao.installTables] Таблица access_tokens создана/проверена.");
            return true;
        } catch (SQLException e) {
            logger.severe("❌ [AccessTokenDao.installTables] Ошибка выполнения SQL: " + e.getMessage());
            return false;
        }
    }

    /**
     * Универсальный метод для выполнения SQL-запросов без возврата результата.
     * Добавлено подробное логирование на каждом этапе (создание Statement, выполнение, результат).
     *
     * @param sql SQL-запрос для выполнения
     * @param successMessage Сообщение для логирования в случае успеха
     * @return true, если запрос выполнен успешно, иначе false
     */
    private boolean executeStatement(String sql, String successMessage) {
        logger.info("executeStatement() -> Создаём Statement для выполнения запроса...");
        try (Statement statement = connection.createStatement()) {
            logger.info("executeStatement() -> Выполняем запрос:\n" + sql);
            statement.executeUpdate(sql);
            logger.info(successMessage);
            return true;
        } catch (SQLException e) {
            logger.severe("❌ [executeStatement] Ошибка выполнения SQL: " + e.getMessage());
            return false;
        }
    }
}