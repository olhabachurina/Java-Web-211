package itstep.learning.services.kdf;

import itstep.learning.services.hash.HashService;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PbKdfService implements KdfService {
    private final int iterationCount = 10000; // Количество итераций
    private final int keyLength = 256; // Длина ключа

    @Override
    public String dk(String password, String salt) {
        try {
            // Алгоритм PBKDF2 с HMAC SHA-256
            String algorithm = "PBKDF2WithHmacSHA256";

            // Генерация производного ключа
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), iterationCount, keyLength);
            byte[] hash = factory.generateSecret(spec).getEncoded();

            // Преобразование в шестнадцатеричную строку
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при генерации ключа: " + e.getMessage(), e);
        }
    }
}