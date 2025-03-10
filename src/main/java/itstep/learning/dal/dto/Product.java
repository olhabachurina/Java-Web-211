package itstep.learning.dal.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Product {
    private UUID productId;
    private String name;
    private String description;
    private double price;
    private String code;
    private int stock;
    private UUID categoryId;
    private String imageId;



    public Product() {
        this.productId = UUID.randomUUID();
    }


    public Product(String name, String description, double price, String code, int stock, UUID categoryId, String imageId) {
        this.productId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.price = price;
        this.code = code;
        this.stock = stock;
        this.categoryId = categoryId;
        this.imageId = imageId;
    }

    // ✅ Метод `fromResultSet()` для создания объекта из БД
    public static Product fromResultSet(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(UUID.fromString(rs.getString("product_id")));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getDouble("price"));
        product.setCode(rs.getString("code"));
        product.setStock(rs.getInt("stock"));
        product.setCategoryId(UUID.fromString(rs.getString("category_id")));
        product.setImageId(rs.getString("image_id"));
        return product;
    }

    // ✅ Геттеры и сеттеры
    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    // ✅ Метод `toString()` для логирования и отладки
    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", code='" + code + '\'' +
                ", stock=" + stock +
                ", categoryId=" + categoryId +
                ", imageId='" + imageId + '\'' +
                '}';
    }
}