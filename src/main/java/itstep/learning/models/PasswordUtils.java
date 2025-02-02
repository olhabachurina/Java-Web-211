package itstep.learning.models;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtils {
    // Метод для хэширования пароля
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12)); // 12 - сложность алгоритма
    }

    // Метод для проверки пароля
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }
}