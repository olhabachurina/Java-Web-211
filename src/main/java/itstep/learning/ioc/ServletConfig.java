package itstep.learning.ioc;
import com.google.inject.servlet.ServletModule;

import itstep.learning.servlets.*;

public class ServletConfig extends ServletModule {
    @Override
    protected void configureServlets() {
        // Фильтр кодировки и CORS


        // Привязываем сервлеты к URL
        serve("/register").with(RegisterServlet.class);
        serve("/login").with(LoginServlet.class);
        serve("/home").with(HomeServlet.class);
        serve("/time").with(TimeServlet.class);
        serve("/random").with(RandomServlet.class);
    }
}

