<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="uk">
<head>
    <meta charset="UTF-8">
    <title>Головна сторінка</title>
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
            cursor: pointer;
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
        .product-form, .product-list {
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
        .product-item {
            background: #2c2c2c;
            padding: 10px;
            border-radius: 5px;
            margin-top: 10px;
        }
        .error-message {
            color: #ff5252;
            font-weight: bold;
        }
        .loading {
            color: #61dafb;
            font-style: italic;
        }
        .hidden {
            display: none;
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

    <!-- Кнопка, которая показывает/скрывает форму добавления товара -->
    <button id="showAddFormBtn" class="btn">➕ Додати товар</button>

    <!-- Форма добавления товара (скрыта по умолчанию) -->
    <div id="addProductFormContainer" class="product-form hidden">
        <h2>Форма додавання товару</h2>
        <!--
             При добавлении (POST) экшен указывает на /products
             Если вы хотите, чтобы форма также использовалась для редактирования (PUT),
             можно динамически менять action и метод через JS
        -->
        <form id="addProductForm" action="http://localhost:8081/Java_Web_211_war/products"
              method="post" enctype="multipart/form-data">
            <label for="name">Назва товару:</label>
            <input type="text" id="name" name="name" required>

            <label for="price">Ціна:</label>
            <input type="number" id="price" name="price" step="0.01" required>

            <label for="description">Опис товару:</label>
            <textarea id="description" name="description"></textarea>

            <label for="code">Код товару:</label>
            <input type="text" id="code" name="code" required>

            <label for="stock">Кількість на складі:</label>
            <input type="number" id="stock" name="stock" required>

            <label for="categoryId">Категорія:</label>
            <select id="categoryId" name="categoryId" required>
                <option value="">🔄 Завантаження категорій...</option>
            </select>

            <label for="file1">Фото товару:</label>
            <input type="file" id="file1" name="file1" accept="image/*">

            <button type="submit">📤 Додати товар</button>
        </form>
    </div>

    <h2>Список товарів</h2>
    <div id="productList" class="product-list loading">🔄 Завантаження товарів...</div>
</div>

<script>
    document.addEventListener("DOMContentLoaded", () => {
        // Кнопка "Добавить товар" - показываем/скрываем форму
        const showAddFormBtn = document.getElementById("showAddFormBtn");
        const addProductFormContainer = document.getElementById("addProductFormContainer");

        showAddFormBtn.addEventListener("click", () => {
            // Переключаем класс .hidden, чтобы показать/скрыть форму
            addProductFormContainer.classList.toggle("hidden");
        });

        // Загружаем категории и товары при старте
        loadCategories();
        loadProducts();
    });

    function loadCategories() {
        const categorySelect = document.getElementById("categoryId");
        categorySelect.innerHTML = '<option value="">🔄 Завантаження категорій...</option>';

        fetch("http://localhost:8081/Java_Web_211_war/categories")
            .then(response => {
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
                return response.json();
            })
            .then(categories => {
                if (!categories.length) {
                    categorySelect.innerHTML = '<option value="">❌ Категорій не знайдено</option>';
                    return;
                }
                categorySelect.innerHTML = '<option value="">-- Оберіть категорію --</option>';
                categories.forEach(category => {
                    const option = document.createElement('option');
                    option.value = category.categoryId;
                    option.textContent = category.categoryTitle;
                    categorySelect.appendChild(option);
                });
            })
            .catch(error => {
                console.error("❌ Помилка при завантаженні категорій:", error);
                categorySelect.innerHTML = '<option value="">❌ Помилка завантаження</option>';
            });
    }

    function loadProducts() {
        const productList = document.getElementById("productList");
        productList.innerHTML = '🔄 Завантаження товарів...';
        productList.classList.add("loading");

        fetch("http://localhost:8081/Java_Web_211_war/products")
            .then(response => {
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
                return response.json();
            })
            .then(products => {
                productList.classList.remove("loading");
                productList.innerHTML = "";

                if (!products.length) {
                    productList.innerHTML = "<p>⚠️ Товарів не знайдено</p>";
                    return;
                }

                products.forEach(product => {
                    const item = document.createElement("div");
                    item.classList.add("product-item");

                    const imageTag = product.imageId
                        ? `<img src="/Java_Web_211_war/storage/${product.imageId}" alt="${product.name}" style="max-width: 100px;">`
                        : '❌ Без зображення';

                    // Кнопки "Редактировать" и "Удалить" (заглушки)
                    // В реальном коде при "Редактировать" можно заполнить форму и отправить PUT
                    // При "Удалить" - отправить DELETE-запрос
                    const editBtn = `<button class="btn" onclick="editProduct('${product.productId}')">✏️ Редагувати</button>`;
                    const deleteBtn = `<button class="btn" onclick="deleteProduct('${product.productId}')">🗑️ Видалити</button>`;


                    productList.appendChild(item);
                });
            })
            .catch(error => {
                console.error("❌ Помилка при завантаженні товарів:", error);
                productList.classList.remove("loading");
                productList.innerHTML = '<p class="error-message">❌ Помилка завантаження товарів</p>';
            });
    }

    // Заглушка для редактирования
    function editProduct(productId) {
        alert("Здесь можно реализовать логику редактирования товара c ID = " + productId);
        // Например:
        // 1) Получить данные товара (если не в массиве) или найти его в загруженных products
        // 2) Заполнить форму, переключиться на PUT-запрос
        // 3) При сабмите формы отправлять PUT ...
    }

    // Заглушка для удаления
    function deleteProduct(productId) {
        if (!confirm("Ви впевнені, що хочете видалити цей товар?")) return;

        // Пример DELETE-запроса
        fetch(`http://localhost:8081/Java_Web_211_war/products?productId=${productId}`, {
            method: "DELETE",
            headers: {
                // Если нужно: 'Authorization': 'Bearer ...'
            }
        })
            .then(response => {
                if (!response.ok) throw new Error(`HTTP error! Status: ${response.status}`);
                return response.json();
            })
            .then(data => {
                console.log("Відповідь при видаленні:", data);
                alert(data.message || "Товар видалено");
                // Перезагружаем список товаров
                loadProducts();
            })
            .catch(error => {
                console.error("❌ Помилка при видаленні товару:", error);
                alert("Сталася помилка при видаленні товару");
            });
    }
</script>

</body>
</html>