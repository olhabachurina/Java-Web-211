package itstep.learning.servlets;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.dal.dao.CategoryDao;
import itstep.learning.dal.dao.ProductDao;
import itstep.learning.dal.dto.Category;
import itstep.learning.dal.dto.Product;
import itstep.learning.services.DbService.DbService;
import itstep.learning.services.form_parse.FormParseResult;
import itstep.learning.services.form_parse.FormParseService;
import itstep.learning.services.storage.StorageService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileItem;
import com.google.gson.Gson;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
/*@WebServlet("/products")*/
public class ProductServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(ProductServlet.class.getName());

    private final FormParseService formParseService;
    private final StorageService storageService;
    private final ProductDao productDao;
    private final CategoryDao categoryDao;

    @Inject
    public ProductServlet(FormParseService formParseService,
                          StorageService storageService,
                          CategoryDao categoryDao,
                          ProductDao productDao) {
        this.formParseService = formParseService;
        this.storageService = storageService;
        this.categoryDao = categoryDao;
        this.productDao = productDao;
        LOGGER.info("🚀 ProductServlet initialized with all dependencies.");
    }

    // ========================
    // ===== CORS SUPPORT =====
    // ========================
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    // ========================
    // ===== GET (PUBLIC) =====
    // ========================
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String servletPath = req.getServletPath();
        String queryString = req.getQueryString();
        String type = req.getParameter("type");

        LOGGER.info("📥 [doGet] Початок обробки GET-запиту:");
        LOGGER.info("➡️ ServletPath: " + servletPath);
        LOGGER.info("➡️ QueryString: " + queryString);
        LOGGER.info("➡️ Type параметр: " + type);

        setCorsHeaders(resp);
        resp.setContentType("application/json;charset=UTF-8");

        try {
            if (type == null || type.isEmpty()) {
                LOGGER.warning("⚠️ [doGet] Не вказано параметр type! Повертаємо всі продукти за замовчуванням.");
                sendJson(resp, productDao.getAllProducts(), "📤 Відправлено всі продукти");
                return;
            }

            switch (type.toLowerCase()) {

                case "categories":
                    LOGGER.info("📂 [doGet] Запит на завантаження всіх категорій");
                    List<Category> categories = categoryDao.getAllCategories();
                    LOGGER.info("✅ Категорій знайдено: " + categories.size());
                    sendJson(resp, categories, "📤 Відправлено всі категорії");
                    break;

                case "paged":
                    LOGGER.info("📄 [doGet] Запит на пагіновану вибірку продуктів");
                    handlePagedProducts(req, resp);
                    break;

                case "category":
                    LOGGER.info("🔎 [doGet] Запит на отримання категорії за id або slug");

                    String slug = req.getParameter("slug");
                    String categoryIdParam = req.getParameter("id");

                    LOGGER.info("➡️ slug: " + slug);
                    LOGGER.info("➡️ categoryId: " + categoryIdParam);

                    Category category = null;

                    if (slug != null && !slug.isEmpty()) {
                        LOGGER.info("🔎 Пошук категорії за SLUG: " + slug);
                        category = categoryDao.getCategoryBySlug(slug);

                    } else if (categoryIdParam != null && !categoryIdParam.isEmpty()) {
                        try {
                            UUID categoryId = UUID.fromString(categoryIdParam);
                            LOGGER.info("🔎 Пошук категорії за ID: " + categoryId);
                            category = categoryDao.getCategoryById(categoryId);
                        } catch (IllegalArgumentException e) {
                            LOGGER.warning("❌ Некоректний UUID для categoryId: " + categoryIdParam);
                            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Невірний формат ID категорії");
                            return;
                        }
                    } else {
                        LOGGER.warning("⚠️ Не вказано параметри 'id' або 'slug' для категорії");
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Потрібно вказати 'id' або 'slug'");
                        return;
                    }

                    if (category == null) {
                        LOGGER.warning("❌ Категорія не знайдена");
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "❌ Категорія не знайдена");
                        return;
                    }

                    String categoryImageUrl = String.format(
                            Locale.ROOT,
                            "%s://%s:%d%s/storage/%s",
                            req.getScheme(),
                            req.getServerName(),
                            req.getServerPort(),
                            req.getContextPath(),
                            category.getCategoryImageId()
                    );
                    LOGGER.info("🖼️ URL зображення категорії: " + categoryImageUrl);
                    category.setCategoryImageId(categoryImageUrl);

                    sendJson(resp, category, "📤 Категорія надіслана успішно");
                    break;

                case "product":
                    LOGGER.info("🛍️ [doGet] Запит на отримання одного продукту за id");

                    String productIdParam = req.getParameter("id");
                    LOGGER.info("➡️ productId: " + productIdParam);

                    if (productIdParam == null || productIdParam.isEmpty()) {
                        LOGGER.warning("⚠️ Не вказано параметр 'id' продукту");
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Потрібно вказати 'id' продукту");
                        return;
                    }

                    try {
                        UUID productId = UUID.fromString(productIdParam);
                        LOGGER.info("🔎 Пошук продукту за ID: " + productId);

                        Product product = productDao.getProductById(productId);

                        if (product == null) {
                            LOGGER.warning("❌ Продукт не знайдено з ID: " + productId);
                            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "❌ Продукт не знайдено");
                            return;
                        }

                        String productImageUrl = String.format(
                                Locale.ROOT,
                                "%s://%s:%d%s/storage/%s",
                                req.getScheme(),
                                req.getServerName(),
                                req.getServerPort(),
                                req.getContextPath(),
                                product.getImageId()
                        );
                        LOGGER.info("🖼️ URL зображення продукту: " + productImageUrl);
                        product.setImageId(productImageUrl);

                        sendJson(resp, product, "📤 Продукт надісланий успішно");

                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("❌ Некоректний UUID для продукту: " + productIdParam);
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Невірний формат ID продукту");
                    }

                    break;

                case "products":
                    LOGGER.info("🛒 [doGet] Запит на отримання всіх продуктів");
                    List<Product> products = productDao.getAllProducts();
                    LOGGER.info("✅ Продуктів знайдено: " + products.size());
                    sendJson(resp, products, "📤 Відправлено всі продукти");
                    break;

                default:
                    LOGGER.warning("❓ Невідомий тип запиту: " + type);
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Невідомий тип запиту: " + type);
                    break;
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ [doGet] Помилка при обробці GET-запиту", e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Помилка сервера");
        }

        LOGGER.info("✅ [doGet] Завершення обробки GET-запиту");
    }


    private void handlePagedProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int limit = Integer.parseInt(req.getParameter("limit"));
        int offset = Integer.parseInt(req.getParameter("offset"));
        String categoryId = req.getParameter("categoryId");

        LOGGER.info("🔎 [handlePagedProducts] Параметры пагинации: limit=" + limit + ", offset=" + offset + ", categoryId=" + categoryId);

        List<Product> products;

        if (categoryId != null && !categoryId.isEmpty()) {
            products = productDao.getProductsByCategoryPaged(UUID.fromString(categoryId), limit, offset);
        } else {
            products = productDao.getProductsPaged(limit, offset, null);
        }

        sendJson(resp, products, "📤 Відправлено продукти з пагінацією");
    }

    private void getCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("📥 [getCategory] Початок обробки запиту на отримання категорії...");

        // Получаем параметр categoryId из запроса
        String categoryIdParam = req.getParameter("id");

        if (categoryIdParam == null || categoryIdParam.isEmpty()) {
            LOGGER.warning("⚠️ [getCategory] Запит без параметра 'id'. Неможливо обробити.");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ ID категорії обов'язковий параметр.");
            return;
        }

        LOGGER.info("🔍 [getCategory] Отримано categoryId з запиту: " + categoryIdParam);

        try {
            // Пробуем сконвертить строку в UUID
            UUID categoryId = UUID.fromString(categoryIdParam);
            LOGGER.info("🆔 [getCategory] Перетворення в UUID успішне: " + categoryId);

            // Получаем категорию из БД по id
            Category category = categoryDao.getCategoryById(categoryId);

            if (category == null) {
                LOGGER.warning("⚠️ [getCategory] Категорія з ID " + categoryId + " не знайдена.");
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "❌ Категорію не знайдено.");
                return;
            }

            LOGGER.info("✅ [getCategory] Категорія знайдена: " + category.getCategoryTitle() + " (ID: " + category.getCategoryId() + ")");

            // Формируем URL для картинки
            String imgUrl = String.format(
                    Locale.ROOT,
                    "%s://%s:%d%s/storage/%s",
                    req.getScheme(),        // http или https
                    req.getServerName(),    // localhost или domain
                    req.getServerPort(),    // порт (например, 8081)
                    req.getContextPath(),   // контекст (например, /Java_Web_211_war)
                    category.getCategoryImageId()
            );

            LOGGER.info("🖼️ [getCategory] Посилання на зображення категорії: " + imgUrl);

            // Обновляем URL картинки в объекте
            category.setCategoryImageId(imgUrl);

            // Отправляем JSON-ответ
            sendJson(resp, category, "📤 [getCategory] Відправлено дані по категорії");

        } catch (IllegalArgumentException ex) {
            LOGGER.warning("❌ [getCategory] Некоректний формат UUID: " + categoryIdParam);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Невірний формат ID категорії.");
        } catch (Exception e) {
            logErrorAndRespond(resp, "❌ [getCategory] Помилка при обробці запиту категорії", e);
        }

        LOGGER.info("✅ [getCategory] Завершення обробки запиту.");
    }

    // ========================
    // ===== POST (CREATE) ===
    // ========================
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("📥 [doPost] Початок обробки POST-запиту (створення продукту).");
        setCorsHeaders(resp);
        if (!isAuthorized(req)) {
            sendUnauthorized(resp);
            LOGGER.info("🚫 [doPost] Запит відхилено через відсутність авторизації.");
            return;
        }
        String savedFileId = null;
        try {
            FormParseResult formParseResult = formParseService.parseRequest(req);
            LOGGER.info("📝 [doPost] Поля форми: " + formParseResult.getFields().keySet());

            // Перевірка обов'язкового поля categoryId
            String catIdStr = formParseResult.getFields().get("categoryId");
            if (catIdStr == null || catIdStr.isEmpty()) {
                LOGGER.warning("⚠️ [doPost] Не вибрано категорію.");
                sendJsonError(resp, "❌ Не вибрано категорію");
                return;
            }

            FileItem file1 = formParseResult.getFiles().get("file1");
            if (file1 != null && file1.getSize() > 0) {
                String fileExt = getFileExtension(file1.getName());
                LOGGER.info("📁 [doPost] Визначено розширення файлу: " + fileExt);
                savedFileId = storageService.put(file1.getInputStream(), fileExt);
                LOGGER.info("✅ [doPost] Файл збережено під ім'ям: " + savedFileId);
            } else {
                LOGGER.warning("❌ [doPost] Картинку не отримали! Я таке не продаю без картинки!");
            }

            String code = formParseResult.getFields().get("code");
            LOGGER.info("🔍 [doPost] Перевірка наявності продукту з кодом: " + code);
            if (productDao.existsByCode(code)) {
                LOGGER.warning("⚠️ [doPost] Товар із кодом " + code + " вже існує.");
                deleteFileIfExists(savedFileId);
                sendJsonError(resp, "❌ Продукт із таким кодом вже існує");
                return;
            }

            Product product = buildProductFromForm(formParseResult, savedFileId);
            LOGGER.info("🛠️ [doPost] Створення продукту: " + product);
            if (productDao.addProduct(product)) {
                LOGGER.info("✅ [doPost] Товар додано успішно!");
                sendJsonSuccess(resp, "✅ Товар додано успішно!");
            } else {
                LOGGER.warning("❌ [doPost] Не вдалося додати товар.");
                deleteFileIfExists(savedFileId);
                sendJsonError(resp, "❌ Не вдалося додати товар");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ [doPost] Аварія при додаванні продукту", e);
            deleteFileIfExists(savedFileId);
            logErrorAndRespond(resp, "❌ Аварія при додаванні продукту", e);
        }
        LOGGER.info("✅ [doPost] Завершення обробки POST-запиту.");
    }

    // ========================
    // ===== PUT (UPDATE) ====
    // ========================
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("📥 [doPut] Початок обробки PUT-запиту (оновлення продукту).");
        setCorsHeaders(resp);
        if (!isAuthorized(req)) {
            sendUnauthorized(resp);
            LOGGER.info("🚫 [doPut] Запит відхилено через відсутність авторизації.");
            return;
        }
        String updatedFileId = null;
        try {
            FormParseResult formParseResult = formParseService.parseRequest(req);
            UUID productId = UUID.fromString(formParseResult.getFields().get("productId"));
            LOGGER.info("🔍 [doPut] Оновлення продукту з ID: " + productId);

            Product existingProduct = productDao.getProductById(productId);
            if (existingProduct == null) {
                LOGGER.warning("❌ [doPut] Продукт не знайдено з ID: " + productId);
                sendJsonError(resp, "❌ Продукт не знайдено");
                return;
            }
            String oldFileId = existingProduct.getImageId();

            FileItem file1 = formParseResult.getFiles().get("file1");
            if (file1 != null && file1.getSize() > 0) {
                String fileExt = getFileExtension(file1.getName());
                LOGGER.info("📁 [doPut] Визначено нове розширення файлу: " + fileExt);
                updatedFileId = storageService.put(file1.getInputStream(), fileExt);
                existingProduct.setImageId(updatedFileId);
                LOGGER.info("✅ [doPut] Нова картинка збережена: " + updatedFileId);
            }

            String code = formParseResult.getFields().get("code");
            LOGGER.info("🔍 [doPut] Перевірка нового коду продукту: " + code);
            if (!existingProduct.getCode().equals(code) && productDao.existsByCode(code)) {
                LOGGER.warning("⚠️ [doPut] Продукт із кодом " + code + " вже існує при оновленні.");
                deleteFileIfExists(updatedFileId);
                sendJsonError(resp, "❌ Продукт із таким кодом вже існує");
                return;
            }

            updateProductFromForm(existingProduct, formParseResult);
            LOGGER.info("🛠️ [doPut] Оновлені дані продукту: " + existingProduct);
            if (productDao.updateProduct(existingProduct)) {
                LOGGER.info("✅ [doPut] Продукт успішно оновлено.");
                deleteOldFileIfNeeded(updatedFileId, oldFileId);
                sendJsonSuccess(resp, "✅ Продукт оновлено");
            } else {
                LOGGER.warning("❌ [doPut] Не вдалося оновити продукт.");
                deleteFileIfExists(updatedFileId);
                sendJsonError(resp, "❌ Не вдалося оновити продукт");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ [doPut] Помилка при оновленні продукту", e);
            deleteFileIfExists(updatedFileId);
            logErrorAndRespond(resp, "❌ Помилка при оновленні продукту", e);
        }
        LOGGER.info("✅ [doPut] Завершення обробки PUT-запиту.");
    }

    // ========================
    // ===== DELETE PRODUCT ===
    // ========================
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("📥 [doDelete] Початок обробки DELETE-запиту (видалення продукту).");
        setCorsHeaders(resp);
        if (!isAuthorized(req)) {
            sendUnauthorized(resp);
            LOGGER.info("🚫 [doDelete] Запит відхилено через відсутність авторизації.");
            return;
        }
        String productIdParam = req.getParameter("productId");
        if (productIdParam == null || productIdParam.isEmpty()) {
            LOGGER.warning("⚠️ [doDelete] Не вказано productId для видалення.");
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Потрібно вказати productId");
            return;
        }
        try {
            UUID productId = UUID.fromString(productIdParam);
            LOGGER.info("🔍 [doDelete] Видалення продукту з ID: " + productId);
            Product product = productDao.getProductById(productId);
            if (product == null) {
                LOGGER.warning("⚠️ [doDelete] Продукт не знайдено з ID: " + productId);
                sendJsonError(resp, "❌ Продукт не знайдено");
                return;
            }
            if (productDao.deleteProductById(productId)) {
                LOGGER.info("✅ [doDelete] Продукт успішно видалено з БД.");
                deleteFileIfExists(product.getImageId());
                sendJsonSuccess(resp, "✅ Продукт та зображення видалено");
            } else {
                LOGGER.warning("❌ [doDelete] Не вдалося видалити продукт з ID: " + productId);
                sendJsonError(resp, "❌ Не вдалося видалити продукт");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ [doDelete] Помилка при видаленні продукту", e);
            logErrorAndRespond(resp, "❌ Помилка при видаленні продукту", e);
        }
        LOGGER.info("✅ [doDelete] Завершення обробки DELETE-запиту.");
    }

    // ========================
    // ===== HELPER METHODS ===
    // ========================
    private void sendUnauthorized(HttpServletResponse resp) throws IOException {
        LOGGER.warning("🚫 [sendUnauthorized] Unauthorized access attempt detected.");
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "❌ Доступ заборонено");
    }

    private String getFileExtension(String filename) {
        String ext = filename.substring(filename.lastIndexOf("."));
        LOGGER.info("📁 [getFileExtension] Extracted file extension: " + ext);
        return ext;
    }

    private void deleteFileIfExists(String fileId) {
        if (fileId != null && !fileId.isEmpty()) {
            storageService.delete(fileId);
            LOGGER.info("🗑️ [deleteFileIfExists] Видалено файл: " + fileId);
        }
    }

    private void deleteOldFileIfNeeded(String updatedFileId, String oldFileId) {
        if (updatedFileId != null && oldFileId != null && !oldFileId.isEmpty()) {
            LOGGER.info("🗑️ [deleteOldFileIfNeeded] Видалення старого файлу з ID: " + oldFileId);
            deleteFileIfExists(oldFileId);
        }
    }

    private Product buildProductFromForm(FormParseResult form, String savedFileId) {
        Product product = new Product();
        product.setProductId(UUID.randomUUID());
        product.setName(form.getFields().get("name"));
        product.setPrice(Double.parseDouble(form.getFields().get("price")));
        product.setDescription(form.getFields().get("description"));
        product.setCode(form.getFields().get("code"));
        product.setStock(Integer.parseInt(form.getFields().get("stock")));
        product.setCategoryId(UUID.fromString(form.getFields().get("categoryId")));
        product.setImageId(savedFileId);
        LOGGER.info("🛠️ [buildProductFromForm] Built product object: " + product);
        return product;
    }

    private void updateProductFromForm(Product product, FormParseResult form) {
        product.setName(form.getFields().get("name"));
        product.setPrice(Double.parseDouble(form.getFields().get("price")));
        product.setDescription(form.getFields().get("description"));
        product.setCode(form.getFields().get("code"));
        product.setStock(Integer.parseInt(form.getFields().get("stock")));
        product.setCategoryId(UUID.fromString(form.getFields().get("categoryId")));
        LOGGER.info("🛠️ [updateProductFromForm] Updated product object with form data: " + product);
    }

    private void sendJson(HttpServletResponse resp, Object data, String logMessage) throws IOException {
        String json = new Gson().toJson(data);
        resp.getWriter().print(json);
        LOGGER.info("📤 " + logMessage + ": " + data);
    }

    private void sendJsonSuccess(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, Map.of("status", "success", "message", message), message);
    }

    private void sendJsonError(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, Map.of("status", "error", "message", message), message);
    }

    private void logErrorAndRespond(HttpServletResponse resp, String logMessage, Exception e) throws IOException {
        LOGGER.log(Level.SEVERE, logMessage, e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Помилка сервера");
    }

    // Авторизація (для POST/PUT/DELETE)
    private boolean isAuthorized(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warning("🚫 [isAuthorized] Немає авторизації або неправильний формат токена!");
            return false;
        }
        String token = authHeader.substring("Bearer ".length());
        boolean isValid = JwtUtil.validateToken(token);
        if (!isValid) {
            LOGGER.warning("🚷 [isAuthorized] Токен недійсний!");
        } else {
            LOGGER.info("✅ [isAuthorized] Токен валідний.");
        }
        return isValid;
    }
}