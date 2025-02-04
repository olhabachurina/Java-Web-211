package itstep.learning.services.random;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Random;

/**
 * Сервіс генерації випадкових чисел, сідування якого базується на поточному часі.
 */
@Singleton
public class TimeBasedRandomService implements RandomService {
    private final DateTimeService dateTimeService;
    private final Random random;

    @Inject
    public TimeBasedRandomService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
        this.random = new Random();
    }

    @Override
    public int randomInt() {
        long timestamp = dateTimeService.getTimestamp(); // Отримуємо поточний час
        random.setSeed(timestamp); // Встановлюємо сід на основі часу
        return random.nextInt(); // Генеруємо випадкове число
    }
}
/**Принцип роботи:

 Сервіс часу (DateTimeService).

 Відповідає за надання поточної часової мітки (timestamp).
 TimeBasedRandomService.

 Використовує DateTimeService для ініціалізації (seed) генератора випадкових чисел.
 Це робить генерацію випадкових чисел передбачуваною за однакових значень часу.
 DI-контейнер (Guice).

 Контейнер створює екземпляри сервісів та впроваджує їх у залежності (сервлети та інші компоненти).
 Сервлет.

 Отримує залежності через DI та використовує їх для обробки запитів.
 Результат роботи сервісу випадкових чисел (пов’язаний із поточним часом) додається до JSON-відповіді, яка повертається клієнту.*/
