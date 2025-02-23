package itstep.learning.ioc;
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger logger = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("AppContextListener: Контекст приложения инициализирован.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Корректно завершаем поток очистки заброшенных соединений MySQL
        AbandonedConnectionCleanupThread.checkedShutdown();
        logger.info("AppContextListener: MySQL AbandonedConnectionCleanupThread успешно остановлен.");
        logger.info("AppContextListener: Контекст приложения остановлен.");
    }
}
