package itstep.learning.dal.dao;


import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Key;
import io.jsonwebtoken.security.Keys;

import com.google.inject.Inject;

import com.google.inject.Singleton;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import itstep.learning.dal.dto.UserAccess;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.config.ConfigService;


@Singleton
public class AccessTokenDao {

    private final DbService dbService;
    private static final Logger logger = Logger.getLogger(AccessTokenDao.class.getName());

    @Inject
    public AccessTokenDao(DbService dbService) {
        this.dbService = dbService;
        logger.info("✅ AccessTokenDao создан с использованием DbService.");
    }

    private static final String SQL_IS_TOKEN_VALID =
            "SELECT COUNT(*) FROM access_tokens WHERE access_token_id = ? AND user_access_id = ? AND expires_at > NOW()";
    private static final String SQL_SAVE_TOKEN =
            "INSERT INTO access_tokens (access_token_id, user_access_id, issued_at, expires_at) VALUES (?, ?, ?, ?)";
    private static final String SQL_UPDATE_TOKEN =
            "UPDATE access_tokens SET access_token_id = ?, issued_at = ?, expires_at = ? WHERE user_access_id = ?";
    private static final String SQL_GET_TOKEN =
            "SELECT access_token_id, expires_at FROM access_tokens WHERE user_access_id = ? AND expires_at > NOW()";
    private static final String SQL_DELETE_TOKEN =
            "DELETE FROM access_tokens WHERE access_token_id = ?";

    public boolean isTokenValid(String token, String userId) {
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_IS_TOKEN_VALID)) {
            stmt.setString(1, token);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    logger.info("✅ Токен действителен: " + token);
                    return true;
                }
            }
            logger.warning("❌ Недействительный токен: " + token);
            return false;
        } catch (SQLException e) {
            logger.severe("❌ Ошибка при проверке токена: " + e.getMessage());
            return false;
        }
    }

    public boolean saveToken(String token, String userId, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_SAVE_TOKEN)) {
            stmt.setString(1, token);
            stmt.setString(2, userId);
            stmt.setTimestamp(3, Timestamp.valueOf(issuedAt));
            stmt.setTimestamp(4, Timestamp.valueOf(expiresAt));
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                logger.info("✅ Токен успешно сохранён в БД: " + token);
                return true;
            } else {
                logger.warning("⚠️ Токен не был сохранён!");
                return false;
            }
        } catch (SQLException e) {
            logger.severe("❌ Ошибка сохранения токена: " + e.getMessage());
            return false;
        }
    }

    public boolean updateToken(String newToken, String userId, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE_TOKEN)) {
            stmt.setString(1, newToken);
            stmt.setTimestamp(2, Timestamp.valueOf(issuedAt));
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.setString(4, userId);
            int rows = stmt.executeUpdate();
            logger.info("✅ Токен обновлен для user_id=" + userId);
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("❌ Ошибка при обновлении токена: " + e.getMessage());
            return false;
        }
    }

    public String getToken(String userId) {
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_GET_TOKEN)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String token = rs.getString("access_token_id");
                    Timestamp expiresAtTimestamp = rs.getTimestamp("expires_at");
                    LocalDateTime expiresAt = expiresAtTimestamp.toLocalDateTime();
                    logger.info("✅ Действующий токен найден: " + token +
                            ", срок действия до: " + expiresAt);
                    return token;
                }
                logger.info("🔍 Токен не найден. Будет создан новый токен.");
                return null;
            }
        } catch (SQLException e) {
            logger.severe("❌ Ошибка при получении токена: " + e.getMessage());
            return null;
        }
    }

    public boolean deleteToken(String token) {
        try (Connection conn = dbService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_DELETE_TOKEN)) {
            stmt.setString(1, token);
            int rows = stmt.executeUpdate();
            logger.info("✅ Токен удалён: " + token);
            return rows > 0;
        } catch (SQLException e) {
            logger.severe("❌ Ошибка при удалении токена: " + e.getMessage());
            return false;
        }
    }
}