package itstep.learning.ioc;
import com.google.inject.servlet.ServletModule;
import itstep.learning.servlets.HomeServlet;
import itstep.learning.servlets.RegisterServlet;
import itstep.learning.servlets.TimeServlet;

public class ServletConfig extends ServletModule {
    @Override
    protected void configureServlets() {
        bind(TimeServlet.class).in(com.google.inject.Singleton.class);
        // Прив'язка сервлетов к URL-шаблонам
        serve("/register").with(RegisterServlet.class);
        serve("/home").with(HomeServlet.class);
        serve("/time").with(TimeServlet.class);

    }
}

