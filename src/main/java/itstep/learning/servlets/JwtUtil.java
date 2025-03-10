package itstep.learning.servlets;

import com.google.inject.Key;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.util.Date;
import java.util.logging.Logger;

import static io.jsonwebtoken.SignatureAlgorithm.*;

public class JwtUtil {
private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());

// 🔐 Секретний ключ (краще зберігати в environment variables або config)
private static final String SECRET_KEY = "SuperSecretKeyForJwtGenerationThatIsVerySecure123!";

// Створюємо ключ для підпису
private static final Key KEY = (Key) Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

// Термін життя токену в мілісекундах (1 година)
private static final long EXPIRATION_TIME = 60 * 60 * 1000;

// ✅ Генерація токену
public static String generateToken(String userId, String username, String role) {
    LOGGER.info("🔑 Генерація токену для користувача: " + username);

    return Jwts.builder()
            .setSubject(userId)                // Унікальний ідентифікатор користувача
            .claim("username", username)       // Додаткові claims
            .claim("role", role)               // Роль користувача
            .setIssuedAt(new Date())           // Коли видано токен
            .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Коли закінчується
            .signWith((java.security.Key) KEY, HS256) // Підпис
            .compact();
}

// ✅ Перевірка токену
public static boolean validateToken(String token) {
    try {
        Jwts.parserBuilder()
                .setSigningKey((java.security.Key) KEY)
                .build()
                .parseClaimsJws(token);

        LOGGER.info("✅ Токен валідний!");
        return true;

    } catch (ExpiredJwtException e) {
        LOGGER.warning("❌ Термін дії токену закінчився!");
    } catch (JwtException e) {
        LOGGER.warning("❌ Токен недійсний! Хтось щось мутить...");
    }

    return false;
}

// ✅ Отримати userId з токену
public static String getUserIdFromToken(String token) {
    Claims claims = getClaims(token);
    return claims != null ? claims.getSubject() : null;
}

// ✅ Отримати username з токену
public static String getUsernameFromToken(String token) {
    Claims claims = getClaims(token);
    return claims != null ? claims.get("username", String.class) : null;
}

// ✅ Отримати роль з токену
public static String getUserRoleFromToken(String token) {
    Claims claims = getClaims(token);
    return claims != null ? claims.get("role", String.class) : null;
}

// ✅ Приватний метод для парсингу claims
private static Claims getClaims(String token) {
    try {
        return Jwts.parserBuilder()
                .setSigningKey(String.valueOf(KEY))
                .build()
                .parseClaimsJws(token)
                .getBody();

    } catch (JwtException e) {
        LOGGER.warning("❌ Не вдалося отримати claims з токену: " + e.getMessage());
        return null;
    }
}
}
