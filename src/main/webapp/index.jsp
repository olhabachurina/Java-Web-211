<%--
    Created by IntelliJ IDEA.
    User: Lector
    Date: 22.01.2025
    Time: 18:45
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Головна сторінка</title>
    <meta charset="UTF-8">
    <style>
        body {
            font-family: Arial, sans-serif;
            text-align: center;
            background-color: #121212;
            color: white;
            margin: 0;
            padding: 20px;
        }
        .nav {
            margin-bottom: 30px;
            padding: 15px;
            background: #1e1e1e;
            border-radius: 10px;
            display: inline-block;
        }
        .btn {
            display: inline-block;
            background-color: #61dafb;
            color: #121212;
            padding: 10px 20px;
            margin: 10px;
            border: none;
            border-radius: 5px;
            text-decoration: none;
            font-size: 16px;
            transition: 0.3s;
        }
        .btn:hover {
            background-color: #4a9ecf;
        }
        .container {
            max-width: 600px;
            margin: 0 auto;
            background: #1e1e1e;
            padding: 20px;
            border-radius: 10px;
            box-shadow: 0 0 10px rgba(255, 255, 255, 0.2);
        }
        h1, h2 {
            color: #61dafb;
        }
        p, b, i {
            font-size: 18px;
        }
        ul {
            text-align: left;
            display: inline-block;
        }
        /* Стилі для форми додавання товару */
        .product-form {
            background: #2c2c2c;
            padding: 20px;
            border-radius: 10px;
            margin-top: 20px;
            text-align: left;
        }
        .product-form label {
            display: block;
            margin-top: 10px;
            color: #61dafb;
        }
        .product-form input,
        .product-form textarea,
        .product-form select {
            width: 100%;
            padding: 8px;
            margin-top: 5px;
            border: 1px solid #61dafb;
            border-radius: 5px;
            background-color: #333;
            color: white;
        }
        .product-form button {
            margin-top: 15px;
            background-color: #61dafb;
            color: black;
            padding: 10px 15px;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            font-size: 16px;
        }
        .product-form button:hover {
            background-color: #4a9ecf;
        }
        /* Список товарів */
        .product-list {
            margin-top: 30px;
            background: #1e1e1e;
            padding: 15px;
            border-radius: 10px;
        }
        .product-item {
            background: #2c2c2c;
            padding: 10px;
            border-radius: 5px;
            margin-top: 10px;
            text-align: left;
        }
    </style>
</head>
<body>

<div class="nav">
    <a href="home" class="btn">🏠 Домашня сторінка</a>
    <a href="register" class="btn">✍️ Реєстрація</a>
    <a href="login" class="btn">🔑 Вхід</a>
    <a href="time" class="btn">⏳ Часовий сервіс</a>
    <a href="random?type=salt&length=10" class="btn">🎲 Генерація випадкових даних</a>
    <a href="products" class="btn">🛒 Продукти</a>
</div>

<div class="container">
    <h1>Ласкаво просимо!</h1>

    <h2>Форма додавання товару</h2>

    <form action="http://localhost:8081/Java_Web_211_war/product" method="post" enctype="multipart/form-data" class="product-form">
        <label for="name">Назва товару:</label>
        <input type="text" id="name" name="name" required>

        <label for="price">Ціна:</label>
        <input type="text" id="price" name="price" required>

        <label for="description">Опис товару:</label>
        <textarea id="description" name="description"></textarea>

        <label for="code">Код товару:</label>
        <input type="text" id="code" name="code" required>

        <label for="stock">Кількість на складі:</label>
        <input type="number" id="stock" name="stock" required>

        <!-- Динамічне завантаження категорій -->
        <label for="categoryId">Категорія:</label>
        <select id="categoryId" name="categoryId" required>
            <option value="">🔄 Завантаження категорій...</option>
        </select>

        <label for="file1">Фото товару:</label>
        <input type="file" id="file1" name="file1" accept="image/*">

        <button type="submit">📤 Додати товар</button>
    </form>

    <!-- Список товарів -->
    <h2>Список товарів</h2>
    <div id="productList" class="product-list">
        <p>🔄 Завантаження товарів...</p>
    </div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", function () {
        const categorySelect = document.getElementById("categoryId");
        const productList = document.getElementById("productList");

        console.log("🔧 Ініціалізація завантаження категорій...");
        fetch("http://localhost:8081/Java_Web_211_war/products")
            .then(response => response.json())
            .then(products => {
                console.log("✅ Товари успішно завантажені:", products);
                productList.innerHTML = "";

                if (!products.length) {
                    productList.innerHTML = "<p>⚠️ Товари не знайдено</p>";
                    return;
                }

                products.forEach(product => {
                    const {
                        name,
                        price,
                        description,
                        code,
                        stock,
                        categoryId,
                        imageId
                    } = product;

                    const productItem = document.createElement("div");
                    productItem.classList.add("product-item");

                    const imageTag = imageId
                        ? `<img src="/Java_Web_211_war/images/${imageId}" alt="${name}" style="max-width: 100px;">`
                        : '❌ Без зображення';

                    productItem.innerHTML = `
                <p>
                    <strong>📦 ${name}</strong><br>
                    💰 Ціна: ${price} грн<br>
                    📄 Опис: ${description}<br>
                    🆔 Код: ${code}<br>
                    📦 Запас: ${stock}<br>
                    📂 Категорія ID: ${categoryId}<br>
                    ${imageTag}
                </p>
            `;

                    productList.appendChild(productItem);
                });
            })
            .catch(error => {
                console.error("❌ Помилка при завантаженні товарів:", error);
                productList.innerHTML = "<p>❌ Помилка завантаження товарів</p>";
            });
    });
</script>

</body>
</html>
