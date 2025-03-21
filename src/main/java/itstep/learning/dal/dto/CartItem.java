package itstep.learning.dal.dto;

import java.util.Objects;
import java.util.UUID;

public class CartItem  {

    private UUID cartItemId;
    private UUID cartId;
    private UUID productId;
    private UUID actionId; // Если у тебя нет этого поля на бэке, убери его
    private double cartItemPrice;
    private short quantity;

    // ✅ Пустой конструктор (нужен для Hibernate, JSON-сериализации и т.д.)
    public CartItem() {
    }

    // ✅ Полный конструктор
    public CartItem(UUID cartItemId, UUID cartId, UUID productId, UUID actionId, double cartItemPrice, short quantity) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.productId = productId;
        this.actionId = actionId;
        this.cartItemPrice = cartItemPrice;
        this.quantity = quantity;
    }

    // ✅ Геттеры и сеттеры
    public UUID getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(UUID cartItemId) {
        this.cartItemId = cartItemId;
    }

    public UUID getCartId() {
        return cartId;
    }

    public void setCartId(UUID cartId) {
        this.cartId = cartId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getActionId() {
        return actionId;
    }

    public void setActionId(UUID actionId) {
        this.actionId = actionId;
    }

    public double getCartItemPrice() {
        return cartItemPrice;
    }

    public void setCartItemPrice(double cartItemPrice) {
        this.cartItemPrice = cartItemPrice;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    // ✅ toString для логирования
    @Override
    public String toString() {
        return "CartItem{" +
                "cartItemId=" + cartItemId +
                ", cartId=" + cartId +
                ", productId=" + productId +
                ", actionId=" + actionId +
                ", cartItemPrice=" + cartItemPrice +
                ", quantity=" + quantity +
                '}';
    }

    // ✅ equals() и hashCode() (обычно по cartItemId)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CartItem)) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(cartItemId, cartItem.cartItemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cartItemId);
    }
}

