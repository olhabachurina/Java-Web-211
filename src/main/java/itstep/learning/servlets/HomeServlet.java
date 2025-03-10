package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.CategoryDao;
import itstep.learning.dal.dao.DataContext;
import itstep.learning.dal.dao.ProductDao;
import itstep.learning.dal.dto.Category;
import itstep.learning.dal.dto.Product;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.config.ConfigService;
import itstep.learning.services.hash.HashService;
import itstep.learning.services.hash.Md5HashService;
import itstep.learning.services.kdf.KdfService;
import itstep.learning.services.random.DateTimeService;
import itstep.learning.services.random.RandomService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.gson.Gson;


import java.util.logging.Level;
import java.util.logging.Logger;



@Singleton
/*@WebServlet("/home")*/
public class HomeServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(HomeServlet.class.getName());

    private final RandomService randomService;
    private final DateTimeService dateTimeService;
    private final KdfService kdfService;
    private final DataContext dataContext;
    private final ConfigService configService;
    private final CategoryDao categoryDao;  // ✅ Додано CategoryDao
    private final ProductDao productDao;
    @Inject
    public HomeServlet(RandomService randomService,
                       DateTimeService dateTimeService,
                       KdfService kdfService,
                       DataContext dataContext,
                       ConfigService configService,
                       CategoryDao categoryDao, ProductDao productDao) { // ✅ Інжектимо CategoryDao
        this.randomService = randomService;
        this.dateTimeService = dateTimeService;
        this.kdfService = kdfService;
        this.dataContext = dataContext;
        this.configService = configService;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
    }



    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        setCorsHeaders(resp);

        Map<String, Object> response = new HashMap<>();
        int statusCode = 200;

        try {
            // ✅ Перевірка, чи всі залежності завантажені
            if (randomService == null || dateTimeService == null || kdfService == null
                    || dataContext == null || configService == null || categoryDao == null || productDao == null) {
                throw new IllegalStateException("❌ Не вдалося завантажити всі залежності.");

            }

            // ✅ Створення таблиць
            boolean tablesCreated = dataContext.installTables();
            String tablesMessage = tablesCreated
                    ? "install ok"
                    : "❌ Помилка при створенні таблиць. Перевірте логи.";

            // ✅ Створення таблиці категорій
            boolean categoriesTableCreated = categoryDao.installTables();
            String categoriesMessage = categoriesTableCreated
                    ? "✅ Таблиця 'categories' успішно створена."
                    : "❌ Помилка при створенні таблиці 'categories'.";

            // ✅ Отримання категорій
            List<Category> categories = categoryDao.getAllCategories();
            int categoriesCount = categories.size();
            boolean productsTableCreated = productDao.installTables(); // ✅ Добавлен вызов installTables() для products
            String productsMessage = productsTableCreated ? "✅ Таблиця 'products' створена." : "❌ Помилка при створенні 'products'.";
            // ✅ Отримання інших параметрів
            int lifetime = configService.getInt("jwt.lifetime");
            String db = configService.getString("db.MySql.dbms");
            String host = configService.getString("db.MySql.host");
            String storagePath = configService.getString("storage.path");
            List<Product> products = productDao.getAllProducts();
            int productsCount = products.size(); // ✅ Количество товаров
            int randomNumber = randomService.randomInt();
            String randomString = randomService.randomString(9);
            String randomFileName = randomService.randomFileName(12);
            String hashedMessage = kdfService.dk("123", "456");

            // ✅ Отримання часу та баз даних
            String currentTime = dataContext.getUserDao().fetchCurrentTime();
            String databases = dataContext.getUserDao().fetchDatabases();

            // ✅ Формування відповіді
            response.put("tablesMessage", tablesMessage);
            response.put("categoriesMessage", categoriesMessage);
            response.put("categoriesCount", categoriesCount); // ✅ Кількість категорій
            response.put("productsMessage", productsMessage);
            response.put("productsCount", productsCount);
            response.put("currentTime", currentTime != null ? currentTime : "");
            response.put("databases", databases != null ? databases : "");
            response.put("randomNumber", randomNumber);
            response.put("randomString", randomString);
            response.put("randomFileName", randomFileName);
            response.put("hashedMessage", hashedMessage);
            response.put("message", "Запит виконано успішно.");
            response.put("lifetime", lifetime);
            response.put("db", db);
            response.put("host", host);
            response.put("storagePath", storagePath);
            response.put("status", statusCode);

        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Помилка завантаження залежностей", e);
            response.put("message", "❌ Помилка завантаження залежностей: " + e.getMessage());
            statusCode = 500;
            response.put("status", statusCode);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Несподівана помилка", e);
            response.put("message", "❌ Виникла несподівана помилка: " + e.getMessage());
            statusCode = 500;
            response.put("status", statusCode);
        }

        resp.setStatus(statusCode);
        resp.getWriter().print(new Gson().toJson(response));
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
}
/**
 * IoC (Inversion of Control) — Інверсія управління
 * Архітектурний патерн, за яким управління (життєвим циклом об'єктів)
 * передається спеціалізованому модулю (контейнер служб, інжектор, Resolver).
 *
 * Основні етапи:
 *
 * 1. Реєстрація:
 *    Додавання інформації до контейнера, зазвичай у формі:
 *    [тип - час життя (scope)]
 *    Приклад:
 *    - Тип 1
 *    - Тип 2
 *    - Тип 3
 *
 *    Контейнер відповідає за управління цими об'єктами.
 *
 * 2. Resolve:
 *    Клас -> (через контейнер) -> Об'єкт (у тому числі вже існуючий, а не новий).
 *    Наприклад:
 *      Connection
 *
 * Приклад коду для впровадження залежностей:
 *
 * class SomeService {
 *     private final Connection _conn;
 *     private final Logger _logger;
 *
 *     public SomeService(Connection c, Logger logger) {
 *         _conn = c;
 *         _logger = logger;
 *     }
 * }
 *


 Асинхронне виконання коду

 1. Синхронне виконання – послідовне виконання операцій у часі:
 ++++++++++------------
 ********************

 2. Асинхронність – будь-яке відхилення від синхронного виконання:
 ++++++++
 --------  // Паралельне виконання
 ********

 ++++++++--------
 ********  // Ниткове виконання

 +   +
 -   -  // Паралельне з перемиканням
 *   *

 -----------------------------------------------
 Способи реалізації асинхронності:
 - **Багатозадачність** – виконання кількох завдань одночасно (Task, Future).
 - **Багатопотоковість** – використання кількох потоків в межах одного процесу.
 - **Багатопроцесність** – виконання декількох процесів одночасно.
 - **Мережеві технології** – асинхронний обмін даними (Grid, Network).

 -----------------------------------------------
 Асинхронність у різних мовах:

 JavaScript (ES6+):
 async function fun() { ... }

 let res = await fun();  // X

 let task = fun();  // Запускаємо без очікування
 інший код...       // Виконується інший код
 res = await task;  // !

 Використання `then()` з `await`:
 res = await fun().then(...).then(...);  // ?

 fun().then(...).then(...).then(res => ... );  // !

 -----------------------------------------------
 Основні проблеми при роботі з асинхронністю:
 - **Callback Hell** – надмірне вкладення викликів, ускладнює читання коду.
 - **Race Condition** – конфлікти при одночасному доступі до спільних ресурсів.
 - **Deadlock** – стан, коли процеси чекають одне одного і не можуть продовжити роботу.
 - **Starvation** – ситуація, коли один процес не отримує ресурсів через інші завдання.

 -----------------------------------------------
 Приклад асинхронного запиту до API:

 async function fetchData() {
 try {
 let response = await fetch("https://api.example.com/data");
 let data = await response.json();
 console.log("Отримані дані:", data);
 } catch (error) {
 console.error("Помилка запиту:", error);
 }
 }
 fetchData();

 -----------------------------------------------
 Коли використовувати асинхронне програмування:
 - Робота з базами даних (запити до сервера).
 - Виконання тривалих операцій без блокування інтерфейсу.
 - Обробка великого обсягу даних у фоновому режимі.
 - Паралельні обчислення для покращення продуктивності.
 */