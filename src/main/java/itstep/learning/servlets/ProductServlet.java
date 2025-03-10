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

@Singleton
/*@WebServlet("/product")*/
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
    }

    // 🔐 Перевірка авторизації (дуже проста, для прикладу)
    private boolean isAuthorized(HttpServletRequest req) {
        String authHeader = req.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warning("🚫 Немає авторизації або неправильний формат токена! Може, не пускати?");
            return false;
        }

        String token = authHeader.substring("Bearer ".length());

        // Тут повинна бути нормальна перевірка JWT або іншого токену.
        boolean isValid = JwtUtil.validateToken(token); // 📝 Псевдокод. Маєш реалізувати сам.
        if (!isValid) {
            LOGGER.warning("🚷 Токен недійсний! Йди гуляй, хлопче!");
        }

        return isValid;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LOGGER.info("📥 Отримано POST-запит на /product");

        if (!isAuthorized(req)) {
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "❌ Доступ заборонено. Тут тільки для своїх!");
            return;
        }

        try {
            // Парсимо форму, бо клієнт може надіслати і файли, і текст
            FormParseResult formParseResult = formParseService.parseRequest(req);

            // Логування отриманих даних
            LOGGER.info("📄 Поля форми: " + formParseResult.getFields().keySet());
            LOGGER.info("📦 Файли форми: " + formParseResult.getFiles().keySet());

            // Обробка файлу картинки
            FileItem file1 = formParseResult.getFiles().get("file1");
            String savedFileId = null;

            if (file1 != null && file1.getSize() > 0) {
                String fileExt = file1.getName().substring(file1.getName().lastIndexOf("."));
                savedFileId = UUID.randomUUID().toString() + fileExt;
                storageService.put(file1.getInputStream(), savedFileId);

                LOGGER.info("✅ Файл збережено під ім'ям: " + savedFileId);
            } else {
                LOGGER.warning("❌ Картинку не отримали! Я таке не продаю без картинки!");
            }

            // Збираємо дані з форми
            String name = formParseResult.getFields().get("name");
            String priceStr = formParseResult.getFields().get("price");
            String description = formParseResult.getFields().get("description");
            String code = formParseResult.getFields().get("code");
            String stockStr = formParseResult.getFields().get("stock");
            String categoryId = formParseResult.getFields().get("categoryId");

            LOGGER.info("🔹 Назва товару: " + name);
            LOGGER.info("💲 Ціна товару: " + priceStr);
            LOGGER.info("📝 Опис товару: " + description);
            LOGGER.info("🔢 Код товару: " + code);
            LOGGER.info("📦 Кількість на складі: " + stockStr);
            LOGGER.info("📂 ID категорії: " + categoryId);

            // Парсимо числові значення (акуратно, бо можуть бути "а що, так можна було?!")
            double price = Double.parseDouble(priceStr);
            int stock = Integer.parseInt(stockStr);
            UUID categoryUuid = UUID.fromString(categoryId);

            // Створюємо продукт
            Product product = new Product();
            product.setProductId(UUID.randomUUID());
            product.setName(name);
            product.setPrice(price);
            product.setDescription(description);
            product.setCode(code);
            product.setStock(stock);
            product.setCategoryId(categoryUuid);
            product.setImageId(savedFileId);

            // Додаємо продукт в БД
            boolean added = productDao.addProduct(product);

            // Формуємо відповідь
            resp.setContentType("application/json;charset=UTF-8");
            Map<String, Object> response = new HashMap<>();

            if (added) {
                LOGGER.info("✅ Продукт '" + name + "' успішно додано до бази даних.");
                response.put("status", "success");
                response.put("message", "✅ Товар додано! Можна святкувати 🎉");
            } else {
                LOGGER.warning("⚠️ Продукт '" + name + "' НЕ додано до бази даних.");
                response.put("status", "error");
                response.put("message", "❌ Товар не додано! Шеф скаже, що ми дарма їмо свій хліб...");
            }

            String json = new Gson().toJson(response);
            resp.getWriter().print(json);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Аварія при додаванні продукту: " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Проблема на сервері! Викликайте спеціаліста з Привозу!");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");

        String type = req.getParameter("type");
        LOGGER.info("📥 GET-запит: /product, параметр type: " + type);

        try {
            if (!isAuthorized(req)) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "❌ Доступ заборонено! Тільки для своїх.");
                return;
            }

            if ("categories".equals(type)) {
                LOGGER.info("📂 Викликаємо getCategories...");
                getCategories(req, resp);
            } else if ("category".equals(type)) {
                String categoryId = req.getParameter("id");
                if (categoryId == null || categoryId.isEmpty()) {
                    LOGGER.warning("⚠️ categoryId не вказано! А як же ми знайдемо таку категорію?");
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ ID категорії не вказано");
                    return;
                }
                LOGGER.info("📂 Викликаємо getCategory з ID: " + categoryId);
                getCategory(req, resp);
            } else {
                LOGGER.info("🛍️ Викликаємо getProducts...");
                getProducts(req, resp);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Критична помилка в doGet: " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Щось пішло не так на сервері...");
        }
    }

    private void getCategories(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Category> categories = new ArrayList<>();
        try {
            categories = categoryDao.getAllCategories();
            LOGGER.info("✅ Завантажено категорії: " + categories.size());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Не вдалося завантажити категорії: " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Проблема з категоріями. Чекаємо понеділка...");
            return;
        }

        String json = new Gson().toJson(categories);
        LOGGER.info("📤 Відправляємо категорії клієнту: " + json);
        resp.getWriter().print(json);
    }

    private void getCategory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.warning("⚠️ Метод getCategory ще не реалізовано. Але ми над цим працюємо!");
        resp.getWriter().println("🛒 Повертаємо інформацію про категорію (заглушка)...");
    }

    private void getProducts(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("📦 Отримання всіх продуктів з бази...");

        try {
            List<Product> products = productDao.getAllProducts();
            LOGGER.info("✅ Знайдено продуктів: " + products.size());

            String json = new Gson().toJson(products);
            LOGGER.info("📤 Відправляємо продукти: " + json);
            resp.getWriter().print(json);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Сталася помилка при отриманні продуктів: " + e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Щось з продуктами... Може, не свіжі?");
        }
    }
}