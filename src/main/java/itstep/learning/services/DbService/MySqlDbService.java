package itstep.learning.services.DbService;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.services.config.ConfigService;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static itstep.learning.rest.RestResponse.gson;

@Singleton
public class MySqlDbService implements DbService {
    private Connection connection;
    private final ConfigService configService;
    private static final Logger logger = Logger.getLogger(MySqlDbService.class.getName());

    @Inject
    public MySqlDbService(ConfigService configService) {
        this.configService = configService;
    }

    @Override
    public Connection getConnection() {
        if (connection == null || isConnectionClosed()) {
            try {
                // Используем правильные пути к параметрам конфигурации
                String host = configService.getString("db.MySql.host");
                int port = configService.getInt("db.MySql.port");
                String database = configService.getString("db.MySql.schema");
                String user = configService.getString("db.MySql.user");
                String password = configService.getString("db.MySql.password");
                String params = configService.getString("db.MySql.params");

                // Формируем URL подключения
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                        + "?useSSL=false"
                        + "&serverTimezone=UTC"
                        + "&" + params;

                // Загружаем драйвер и устанавливаем соединение
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, password);
                logger.info("✅ Соединение с базой данных установлено успешно: " + url);
            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, "❌ JDBC-драйвер не найден: " + ex.getMessage(), ex);
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "❌ Ошибка подключения к базе данных: " + ex.getMessage(), ex);
            }
        }
        return connection;
    }

    private boolean isConnectionClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка проверки соединения: " + e.getMessage(), e);
            return true;
        }
    }
}