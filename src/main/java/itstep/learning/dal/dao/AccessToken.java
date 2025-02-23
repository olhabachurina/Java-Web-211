package itstep.learning.dal.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class AccessToken {
    private UUID accessToken;
    private UUID userAccessId;
    private Date issuedAt;
    private Date expiresAt;


    public static AccessToken fromResultSet(ResultSet rs) throws SQLException {
        AccessToken token = new AccessToken();

        // Извлекаем access_token_id и преобразуем в UUID
        String accessTokenIdStr = rs.getString("access_token_id");
        if (accessTokenIdStr != null) {
            token.setAccessToken(UUID.fromString(accessTokenIdStr));
        }

        // Извлекаем user_access_id и преобразуем в UUID
        String userAccessIdStr = rs.getString("user_access_id");
        if (userAccessIdStr != null) {
            token.setUserAccessId(UUID.fromString(userAccessIdStr));
        }

        // Извлекаем дату выдачи токена (issued_at)
        Timestamp issuedAtTimestamp = rs.getTimestamp("issued_at");
        if (issuedAtTimestamp != null) {
            token.setIssuedAt(new Date(issuedAtTimestamp.getTime()));
        }

        // Извлекаем дату истечения токена (expires_at)
        Timestamp expiresAtTimestamp = rs.getTimestamp("expires_at");
        if (expiresAtTimestamp != null) {
            token.setExpiresAt(new Date(expiresAtTimestamp.getTime()));
        }

        return token;
    }
    // Геттер для accessToken
    public UUID getAccessToken() {
        return accessToken;
    }

    // Сеттер для accessToken
    public void setAccessToken(UUID accessToken) {
        this.accessToken = accessToken;
    }

    // Геттер для userAccessId
    public UUID getUserAccessId() {
        return userAccessId;
    }

    // Сеттер для userAccessId
    public void setUserAccessId(UUID userAccessId) {
        this.userAccessId = userAccessId;
    }

    // Геттер для issuedAt
    public Date getIssuedAt() {
        return issuedAt;
    }

    // Сеттер для issuedAt
    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    // Геттер для expiresAt
    public Date getExpiresAt() {
        return expiresAt;
    }

    // Сеттер для expiresAt
    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}

