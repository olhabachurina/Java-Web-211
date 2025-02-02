package itstep.learning.services.random;
import java.time.LocalDateTime;
import com.google.inject.Singleton;
import java.time.Instant;

@Singleton
public class DateTimeService {
    public long getTimestamp() {
        return Instant.now().toEpochMilli();
    }
}
