package itstep.learning.ioc;

import com.mysql.cj.jdbc.Driver;
import itstep.learning.dal.dao.AccessTokenDao;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.DbService.MySqlDbService;
import itstep.learning.services.config.ConfigService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;


@WebListener
public class DbConfigListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(DbConfigListener.class.getName());
    private Connection connection;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("DbConfigListener: приложение стартует");

        ServletContext context = sce.getServletContext();
        ConfigService configService = (ConfigService) context.getAttribute("configService");

        if (configService == null) {
            logger.severe("❌ ConfigService не найден в ServletContext! Проверьте инициализацию.");
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String host = configService.getString("db.MySql.host");
            int port = configService.getInt("db.MySql.port");
            String schema = configService.getString("db.MySql.schema");
            String user = configService.getString("db.MySql.user");
            String password = configService.getString("db.MySql.password");
            String params = configService.getString("db.MySql.params");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + schema
                    + "?useSSL=false"
                    + "&serverTimezone=UTC"
                    + "&" + params;

            connection = DriverManager.getConnection(url, user, password);
            context.setAttribute("dbConnection", connection);

            logger.info("✅ Подключение к БД установлено: " + url);
        } catch (ClassNotFoundException ex) {
            logger.log(Level.SEVERE, "❌ JDBC-драйвер не найден: " + ex.getMessage(), ex);
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "❌ Ошибка подключения к базе данных: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (connection != null) {
            try {
                connection.close();
                logger.info("✅ Подключение к БД закрыто.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "❌ Ошибка закрытия соединения: " + e.getMessage(), e);
            }
        }
    }
}