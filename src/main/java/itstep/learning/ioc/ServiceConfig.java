package itstep.learning.ioc;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.DbService.MySqlDbService;
import itstep.learning.services.hash.HashService;
import itstep.learning.services.hash.Md5HashService;
import itstep.learning.services.kdf.KdfService;
import itstep.learning.services.kdf.PbKdfService;
import itstep.learning.services.random.*;

public class ServiceConfig extends AbstractModule {
    @Override
    protected void configure() {
        bind(HashService.class).to(Md5HashService.class);
        bind(KdfService.class).to(PbKdfService.class);
        //bind(RandomService.class).to(TimeBasedRandomService.class);
        bind(DateTimeService.class).in(Singleton.class);
        bind(DbService.class).to(MySqlDbService.class);
        bind(DataContext.class).in(Singleton.class);
        bind(RandomService.class).to(RandomServiceImpl.class);
    }

}