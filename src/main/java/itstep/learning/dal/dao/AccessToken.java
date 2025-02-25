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

        String accessTokenIdStr = rs.getString("access_token_id");
        if (accessTokenIdStr != null) {
            token.setAccessToken(UUID.fromString(accessTokenIdStr));
        }

        String userAccessIdStr = rs.getString("user_access_id");
        if (userAccessIdStr != null) {
            token.setUserAccessId(UUID.fromString(userAccessIdStr));
        }

        Timestamp issuedAtTimestamp = rs.getTimestamp("issued_at");
        if (issuedAtTimestamp != null) {
            token.setIssuedAt(new Date(issuedAtTimestamp.getTime()));
        }

        Timestamp expiresAtTimestamp = rs.getTimestamp("expires_at");
        if (expiresAtTimestamp != null) {
            token.setExpiresAt(new Date(expiresAtTimestamp.getTime()));
        }

        return token;
    }

    public UUID getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(UUID accessToken) {
        this.accessToken = accessToken;
    }

    public UUID getUserAccessId() {
        return userAccessId;
    }

    public void setUserAccessId(UUID userAccessId) {
        this.userAccessId = userAccessId;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }
}