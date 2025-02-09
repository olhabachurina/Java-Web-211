package itstep.learning.services.random;

import com.google.inject.Singleton;
import java.util.Random;

@Singleton
public class UtilRandomService implements RandomService {
    private final Random random = new Random();
    private static final String FILE_NAME_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final String GENERAL_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+=-<>";

    @Override
    public int randomInt() {
        return random.nextInt();
    }

    /**
     * Генерирует случайную строку заданной длины.
     *
     * @param length длина строки
     * @return случайная строка
     */
    public String randomString(int length) {
        return generateRandomString(GENERAL_CHARACTERS, length);
    }

    /**
     * Генерирует случайное имя файла заданной длины.
     *
     * @param length длина имени файла
     * @return случайное имя файла
     */
    public String randomFileName(int length) {
        return generateRandomString(FILE_NAME_CHARACTERS, length);
    }

    /**
     * Генерация случайной строки из заданного набора символов.
     *
     * @param characters набор символов
     * @param length     длина строки
     * @return случайная строка
     */
    private String generateRandomString(String characters, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            result.append(characters.charAt(index));
        }
        return result.toString();
    }
}