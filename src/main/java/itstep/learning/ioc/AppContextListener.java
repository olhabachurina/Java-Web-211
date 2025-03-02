package itstep.learning.ioc;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Logger;


import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Logger;

@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Контекст приложения инициализирован");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Контекст приложения уничтожается, очистка ресурсов");

        // Отписка зарегистрированных JDBC драйверов
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                LOGGER.info("JDBC driver deregistered: " + driver);
            } catch (SQLException e) {
                LOGGER.severe("Ошибка при deregister JDBC driver " + driver + ": " + e.getMessage());
            }
        }

        // Попытка остановить фоновый поток MySQL Connector/J через рефлексию, если требуется
        try {
            // Пример вызова приватного метода shutdown(boolean) через рефлексию:
            java.lang.reflect.Method shutdownMethod =
                    com.mysql.cj.jdbc.AbandonedConnectionCleanupThread.class.getDeclaredMethod("shutdown", boolean.class);
            shutdownMethod.setAccessible(true);
            shutdownMethod.invoke(null, true);
            LOGGER.info("AbandonedConnectionCleanupThread успешно остановлен");
        } catch (Exception e) {
            LOGGER.severe("Ошибка при остановке AbandonedConnectionCleanupThread: " + e.getMessage());
        }
    }
}
/*@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Application context initialized.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Завершаем поток очистки соединений
        try {
            AbandonedConnectionCleanupThread.checkedShutdown();
            logger.info("MySQL AbandonedConnectionCleanupThread successfully shut down.");
        } catch (Exception e) {
            logger.severe("Error shutting down MySQL cleanup thread: " + e.getMessage());
        }

        // Deregister JDBC drivers
        Enumeration<java.sql.Driver> drivers = DriverManager.getDrivers();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (drivers.hasMoreElements()) {
            Driver driver = (Driver) drivers.nextElement();
            if (driver.getClass().getClassLoader() == cl) {
                try {
                    DriverManager.deregisterDriver(driver);
                    logger.info("Deregistered JDBC driver: " + driver);
                } catch (SQLException ex) {
                    logger.severe("Error deregistering JDBC driver: " + ex.getMessage());
                }
            } else {
                logger.info("Not deregistering JDBC driver as it does not belong to this ClassLoader: " + driver);
            }
        }
        logger.info("Application context destroyed.");
    }
}*/