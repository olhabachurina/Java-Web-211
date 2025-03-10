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
        try {
            String host = configService.getString("db.MySql.host");
            int port = configService.getInt("db.MySql.port");
            String database = configService.getString("db.MySql.schema");
            String user = configService.getString("db.MySql.user");
            String password = configService.getString("db.MySql.password");
            String params = configService.getString("db.MySql.params");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=false"
                    + "&serverTimezone=UTC"
                    + "&allowPublicKeyRetrieval=true"
                    + "&" + params;

            Class.forName("com.mysql.cj.jdbc.Driver");
            logger.info("✅ Устанавливаем новое соединение с БД: " + url);
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            logger.log(Level.SEVERE, "❌ Ошибка подключения к базе данных: " + e.getMessage(), e);
            throw new RuntimeException("Не удалось подключиться к БД", e);
        }
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