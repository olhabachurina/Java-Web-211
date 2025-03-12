package itstep.learning.ioc;
import com.google.inject.servlet.ServletModule;

import itstep.learning.servlets.*;

public class ServletConfig extends ServletModule {
    @Override
    protected void configureServlets() {

        serve("/register").with(RegisterServlet.class);
        serve("/login").with(LoginServlet.class);
        serve("/home").with(HomeServlet.class);
        serve("/time").with(TimeServlet.class);
        serve("/random").with(RandomServlet.class);
        serve("/users/*").with(UserServlet.class);
        serve("/products").with(ProductServlet.class);
        serve("/categories").with(ProductServlet.class);

        serve("/storage/*").with(StorageServlet.class);

    }
}

