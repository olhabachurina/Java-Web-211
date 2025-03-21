package itstep.learning.dal.dto;

import java.time.LocalDateTime;
public class Cart {
    private String cartId;
    private String userAccessId;
    private String roleId;
    private String login;
    private String salt;
    private String dk; // Derived Key
    private LocalDateTime cartCreatedAt;
    private LocalDateTime cartClosedAt;
    private boolean isCancelled;
    private double cartPrice;

    // ✅ Конструктор
    public Cart(String cartId,
                String userAccessId,
                String roleId,
                String login,
                String salt,
                String dk,
                LocalDateTime cartCreatedAt,
                LocalDateTime cartClosedAt,
                boolean isCancelled,
                double cartPrice) {
        this.cartId = cartId;
        this.userAccessId = userAccessId;
        this.roleId = roleId;
        this.login = login;
        this.salt = salt;
        this.dk = dk;
        this.cartCreatedAt = cartCreatedAt;
        this.cartClosedAt = cartClosedAt;
        this.isCancelled = isCancelled;
        this.cartPrice = cartPrice;
    }

    // ✅ Геттеры (методы доступа к полям)

    public String getCartId() {
        return cartId;
    }

    public String getUserAccessId() {
        return userAccessId;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getLogin() {
        return login;
    }

    public String getSalt() {
        return salt;
    }

    public String getDk() {
        return dk; // 👈 или getDerivedKey(), если удобнее называть
    }

    // ✅ Если тебе нужен именно метод getDerivedKey(), а не getDk()
    public String getDerivedKey() {
        return dk;
    }

    public LocalDateTime getCartCreatedAt() {
        return cartCreatedAt;
    }

    public LocalDateTime getCartClosedAt() {
        return cartClosedAt;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public double getCartPrice() {
        return cartPrice;
    }

    // ✅ Можно добавить toString() для удобства отладки
    @Override
    public String toString() {
        return "Cart{" +
                "cartId='" + cartId + '\'' +
                ", userAccessId='" + userAccessId + '\'' +
                ", roleId='" + roleId + '\'' +
                ", login='" + login + '\'' +
                ", salt='" + salt + '\'' +
                ", dk='" + dk + '\'' +
                ", cartCreatedAt=" + cartCreatedAt +
                ", cartClosedAt=" + cartClosedAt +
                ", isCancelled=" + isCancelled +
                ", cartPrice=" + cartPrice +
                '}';
    }

    public void setCartId(String cartId) {
    }
}
