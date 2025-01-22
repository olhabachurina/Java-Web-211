<%--
    Created by IntelliJ IDEA.
    User: Lector
    Date: 22.01.2025
    Time: 18:45
--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Title</title>
</head>
<body>
<h1>JSP</h1>
<h2>Вирази</h2>
<p><%= 2 + 3 %></p> <!-- Вираз для вивода результату 2 + 3 -->

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
    <li><%= i + 1 %></li> <!-- Нумерація списка, починая с 1 -->
    <% } %>
</ul>
</body>
</html>
