package itstep.learning.services.random;
import java.time.LocalDateTime;
import com.google.inject.Singleton;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Singleton
public class DateTimeService {
    /**
     * Возвращает текущую метку времени в миллисекундах (эпоха Unix).
     */
    public long getTimestamp() {
        return Instant.now().toEpochMilli();
    }

    /**
     * Возвращает текущую дату и время в формате "yyyy-MM-dd HH:mm:ss".
     */
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
