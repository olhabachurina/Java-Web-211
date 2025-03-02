package itstep.learning.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Singleton;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import itstep.learning.services.config.ConfigService;
import jakarta.inject.Inject;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.Logger;

@Singleton
public class JwtService {

    private final SecretKey secretKey;
    private final long tokenLifetime; // в секундах
    private final Gson gson = new Gson();
    private static final Logger LOGGER = Logger.getLogger(JwtService.class.getName());

    /**
     * Конструктор, що ініціалізує JwtService використовуючи ConfigService для зчитування налаштувань.
     *
     * @param configService сервіс конфігурації, який надає налаштування з файлу конфігурації
     */
    @Inject
    public JwtService(ConfigService configService) {
        LOGGER.info("Ініціалізація JwtService...");
        // Зчитуємо секретний ключ для JWT із конфігурації
        String secret = configService.getString("jwt.secret");
        LOGGER.info("Зчитано значення jwt.secret з конфігурації");

        // Створюємо SecretKey для алгоритму HS256
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        LOGGER.info("SecretKey успішно створено");

        // Зчитуємо час життя токена (в секундах) з конфігурації
        this.tokenLifetime = configService.getInt("jwt.lifetime");
        LOGGER.info("Час життя JWT встановлено: " + tokenLifetime + " секунд");
    }

    /**
     * Створює JWT з довільного об'єкта payload.
     *
     * @param payload об'єкт, який буде серіалізовано у формат JSON і розміщено у полі subject токена
     * @return JWT у вигляді рядка
     */
    public String createJwt(Object payload) {
        LOGGER.info("Початок створення JWT...");
        // Серіалізація об'єкта в JSON
        String jsonPayload = gson.toJson(payload);
        LOGGER.fine("Payload у форматі JSON: " + jsonPayload);

        // Отримання поточного часу
        long nowMillis = System.currentTimeMillis();
        // Обчислення часу закінчення дії токена
        long expMillis = nowMillis + (tokenLifetime * 1000);
        LOGGER.fine("Поточний час (ms): " + nowMillis + ", час закінчення (ms): " + expMillis);

        // Створення JWT за допомогою бібліотеки JJWT
        String jwt = Jwts.builder()
                .setSubject(jsonPayload)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        LOGGER.info("JWT успішно створено");
        return jwt;
    }

    /**
     * Перевіряє JWT та повертає payload у вигляді JsonElement.
     * Якщо токен недійсний або прострочений, повертає null.
     *
     * @param jwtToken рядок JWT
     * @return payload як JsonElement або null
     */
    public JsonElement fromJwt(String jwtToken) {
        LOGGER.info("Початок перевірки JWT...");
        try {
            // Розбір JWT та отримання значення поля subject, яке містить JSON payload
            String jsonPayload = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwtToken)
                    .getBody()
                    .getSubject();
            LOGGER.fine("JWT успішно розібрано, отримано payload: " + jsonPayload);
            // Перетворення JSON рядка у JsonElement
            return JsonParser.parseString(jsonPayload);
        } catch (JwtException e) {
            LOGGER.warning("Невалідний або прострочений JWT: " + e.getMessage());
            return null;
        }
    }
}