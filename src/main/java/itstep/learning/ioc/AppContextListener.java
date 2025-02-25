package itstep.learning.ioc;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import com.mysql.cj.jdbc.Driver;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Logger;

@WebListener
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
}