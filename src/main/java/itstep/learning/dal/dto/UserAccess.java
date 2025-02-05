package itstep.learning.dal.dto;

import java.util.UUID;

public class UserAccess {
    private UUID userAccess;
    private UUID userid;
    private String login;
    private String salt;
    private String dk;
    private String roleId;

    // Getters
    public UUID getUserAccess() {
        return userAccess;
    }

    public UUID getUserid() {
        return userid;
    }

    public String getLogin() {
        return login;
    }

    public String getSalt() {
        return salt;
    }

    public String getDk() {
        return dk;
    }

    public String getRoleId() {
        return roleId;
    }

    // Setters
    public void setUserAccess(UUID userAccess) {
        this.userAccess = userAccess;
    }

    public void setUserid(UUID userid) {
        this.userid = userid;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public void setDk(String dk) {
        this.dk = dk;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
}
// DAL (Data Access Layer)

// MySqlRepo
// Репозиторій для роботи з базою даних MySQL

// DTO (Entity)
// User — об'єкт для передачі даних про користувача
// Token — об'єкт для передачі даних про токен

// DAO
// UserDao — клас для доступу до таблиці `users`
// TokenDao — клас для доступу до таблиці `tokens`

// OracleRepo
// Репозиторій для роботи з базою даних Oracle

// DTO (Entity)
// Product — об'єкт для передачі даних про продукт
// Action — об'єкт для передачі даних про дію

// DAO
// ProductDao — клас для доступу до таблиці `products`
// ActionDao — клас для доступу до таблиці `actions`
