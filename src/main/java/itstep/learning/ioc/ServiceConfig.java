package itstep.learning.ioc;
import com.google.inject.AbstractModule;
import itstep.learning.services.random.DateTimeService;
import itstep.learning.services.random.RandomService;
import itstep.learning.services.random.UtilRandomService;

public class ServiceConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(RandomService.class).to(UtilRandomService.class);
        bind(DateTimeService.class).asEagerSingleton();
    }

}