package itstep.learning.services.hash;

import com.google.inject.Singleton;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


@Singleton
public class Md5HashService implements HashService {

    @Override
    public String digest(String input) {
        try {
            // Створюємо масив символів для збереження результату
            char[] chars = new char[32]; // Хеш MD5 завжди має довжину 32 символи
            int i = 0;

            // Отримуємо MD5-хеш у вигляді масиву байтів
            for (byte b : MessageDigest.getInstance("MD5").digest(input.getBytes())) {
                int bi = b & 0xFF; // Преобразуємо байт у значення від 0 до 255

                // Конвертуємо байт у шістнадцятковий рядок
                String str = Integer.toHexString(bi);

                // Якщо рядок має довжину 1 символ (наприклад, "A"), додаємо провідний 0
                if (bi < 16) {
                    chars[i] = '0'; // Додаємо провідний 0
                    chars[i + 1] = str.charAt(0);
                } else {
                    chars[i] = str.charAt(0); // Перший символ
                    chars[i + 1] = str.charAt(1); // Другий символ
                }

                i += 2; // Зсув на 2 позиції для наступного байта
            }

            // Повертаємо масив символів як рядок
            return new String(chars);

        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Помилка: алгоритм MD5 не знайдено", ex);
        }
    }
}


