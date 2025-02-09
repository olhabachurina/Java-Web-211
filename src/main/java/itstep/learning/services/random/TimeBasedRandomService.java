package itstep.learning.services.random;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Random;

/**
 * Сервіс генерації випадкових чисел, сідування якого базується на поточному часі.
 */
@Singleton
public class TimeBasedRandomService implements RandomService {
    private final DateTimeService dateTimeService; // Сервис для получения времени
    private Random random; // Генератор случайных чисел

    @Inject
    public TimeBasedRandomService(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService; // Внедрение DateTimeService
        updateRandomSeed(); // Инициализируем Random с начальным сидом
    }

    @Override
    public int randomInt() {
        updateRandomSeed(); // Обновляем сид перед генерацией случайного числа
        return random.nextInt();
    }

    /**
     * Генерация случайной строки заданной длины.
     */
    @Override
    public String randomString(int length) {
        updateRandomSeed();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    /**
     * Генерация случайного имени файла заданной длины.
     */
    @Override
    public String randomFileName(int length) {
        updateRandomSeed();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
        StringBuilder result = new StringBuilder("file"); // Добавляем префикс "file"
        for (int i = 0; i < length - 4; i++) { // -4 для учета длины "file"
            result.append(characters.charAt(random.nextInt(characters.length())));
        }
        return result.toString();
    }

    /**
     * Обновление генератора случайных чисел с новым сидом на основе текущего времени.
     */
    private void updateRandomSeed() {
        long timestamp = dateTimeService.getTimestamp(); // Получаем текущий временной штамп
        random = new Random(timestamp); // Обновляем Random с новым сидом
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
