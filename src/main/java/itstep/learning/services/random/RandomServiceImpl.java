package itstep.learning.services.random;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class RandomServiceImpl implements RandomService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Override
    public int randomInt() {
        return RANDOM.nextInt(Integer.MAX_VALUE); // Генерация случайного числа
    }

    @Override
    public String randomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Длина строки должна быть больше 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    @Override
    public String randomFileName(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Длина имени файла должна быть больше 0");
        }
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, length);
        return uuid + ".txt";
    }
}