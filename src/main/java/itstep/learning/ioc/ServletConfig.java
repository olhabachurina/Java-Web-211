package itstep.learning.ioc;
import com.google.inject.servlet.ServletModule;
import itstep.learning.servlets.HomeServlet;
import itstep.learning.servlets.RandomServlet;
import itstep.learning.servlets.RegisterServlet;
import itstep.learning.servlets.TimeServlet;

public class ServletConfig extends ServletModule {
    @Override
    protected void configureServlets() {

        // Привязываем сервлеты к URL-шаблонам
        serve("/register").with(RegisterServlet.class);
        serve("/home").with(HomeServlet.class);
        serve("/time").with(TimeServlet.class);
        serve("/random").with(RandomServlet.class); //
    }
}

