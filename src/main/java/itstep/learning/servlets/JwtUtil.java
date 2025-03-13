package itstep.learning.servlets;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.logging.Logger;

import static io.jsonwebtoken.SignatureAlgorithm.*;
import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;

public class JwtUtil {
    private static final Logger LOGGER = Logger.getLogger(JwtUtil.class.getName());

    private static final String SECRET_KEY = "supersecretkey12345678901234567890"; // Той, що в конфігу
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 година

    public static String generateToken(String userId, String username, String role) {
        LOGGER.info("🔑 Генерація токену для користувача: " + username);

        return Jwts.builder()
                .setSubject(userId)
                .claim("username", username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token);
            LOGGER.info("✅ Токен валідний!");
            return true;
        } catch (JwtException e) {
            LOGGER.warning("❌ Токен недійсний: " + e.getMessage());
            return false;
        }
    }

    public static String getUserIdFromToken(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public static String getUsernameFromToken(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("username", String.class) : null;
    }

    public static String getUserRoleFromToken(String token) {
        Claims claims = getClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    private static Claims getClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            LOGGER.warning("❌ Не вдалося отримати claims з токену: " + e.getMessage());
            return null;
        }
    }
}