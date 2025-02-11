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
    </style>
</head>
<body>

<div class="nav">
    <a href="home" class="btn">🏠 Домашня сторінка</a>
    <a href="register" class="btn">✍️ Реєстрація</a>
    <a href="time" class="btn">⏳ Часовий сервіс</a>
    <a href="random?type=salt&length=10" class="btn">🎲 Генерація випадкових даних</a>
</div>

<div class="container">
    <h1>Головна сторінка</h1>

    <h2>Вирази</h2>
    <p><%= 2 + 3 %></p>

    <h2>Змінні</h2>
    <%
        int x = 10;
    %>
    <p>Значення змінної x: <%= x %></p>

    <h2>Інструкції управління</h2>
    <% if (x % 2 == 0) { %>
    <b>Число <%= x %> парне</b>
    <% } else { %>
    <i>Число <%= x %> непарне</i>
    <% } %>

    <h2>Цикл</h2>
    <ul>
        <% for (int i = 0; i < 10; i++) { %>
        <li><%= i + 1 %></li>
        <% } %>
    </ul>
</div>

</body>
</html>