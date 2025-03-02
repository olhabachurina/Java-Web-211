package itstep.learning.ioc;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.config.ConfigService;
import itstep.learning.services.random.RandomService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class IocContextListener extends GuiceServletContextListener {
    private Injector injector;

    @Override
    protected Injector getInjector() {
        if (injector == null) {
            injector = Guice.createInjector(new ServiceConfig(), new ServletConfig()); // ✅ Добавили ServletConfig
        }
        return injector;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        super.contextInitialized(sce);
        ServletContext context = sce.getServletContext();
        context.setAttribute("configService", injector.getInstance(ConfigService.class));
        context.setAttribute("dbService", injector.getInstance(DbService.class));
        context.setAttribute("randomService", injector.getInstance(RandomService.class));
    }
}
/*
ContextListener — "слухачі" подій створення контексту, тобто запуску/делоаду проєкту.
Можуть вважатися точкою входу/запуску.
*/