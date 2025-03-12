package itstep.learning.servlets;

import com.google.gson.Gson;
import itstep.learning.services.storage.StorageService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Singleton;

@Singleton
/*@WebServlet("/storage/*")*/
public class StorageServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(StorageServlet.class.getName());
    private final StorageService storageService;

    // Чорний список небезпечних розширень
    private static final Set<String> BLACKLISTED_EXTENSIONS = Set.of(
            ".exe", ".php", ".py", ".cgi", ".sh",
            ".bat", ".cmd", ".jsp", ".asp", ".aspx"
    );

    @Inject
    public StorageServlet(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * Для поддержки CORS-запросов (React на другом порту).
     * Браузер перед отправкой PUT/DELETE может делать preflight-запрос OPTIONS.
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    /**
     * GET - Завантажити файл
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);  // Если нужны кроссдоменные запросы
        resp.setCharacterEncoding("UTF-8");

        String fileId = extractFileId(req);
        if (!isValidFileId(fileId)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Некоректний fileId або заборонене розширення");
            return;
        }

        LOGGER.info("📥 Запит на завантаження файлу: " + fileId);

        try (InputStream fileStream = storageService.get(fileId);
             OutputStream out = resp.getOutputStream()) {

            String mimeType = getMimeType(fileId);
            resp.setContentType(mimeType);
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileId + "\"");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            LOGGER.info("✅ Файл успішно віддано: " + fileId + " (MIME: " + mimeType + ")");
        } catch (FileNotFoundException e) {
            LOGGER.warning("❌ Файл не знайдено: " + fileId);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "❌ Файл не знайдено");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Помилка при читанні файлу: " + fileId, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "❌ Помилка при читанні файлу");
        }
    }

    /**
     * DELETE - Видалити файл
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);  // Если нужны кроссдоменные запросы
        resp.setCharacterEncoding("UTF-8");

        String fileId = extractFileId(req);
        if (!isValidFileId(fileId)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Некоректний fileId або заборонене розширення");
            return;
        }

        LOGGER.info("🗑️ Запит на видалення файлу: " + fileId);

        boolean deleted = storageService.delete(fileId);

        resp.setContentType("application/json;charset=UTF-8");
        Map<String, Object> response = new HashMap<>();

        if (deleted) {
            LOGGER.info("✅ Файл видалено: " + fileId);
            response.put("status", "success");
            response.put("message", "✅ Файл видалено");
        } else {
            LOGGER.warning("⚠️ Файл не знайдено для видалення: " + fileId);
            response.put("status", "error");
            response.put("message", "❌ Файл не знайдено або не видалено");
        }

        resp.getWriter().print(new Gson().toJson(response));
    }

    /**
     * Устанавливает заголовки CORS, чтобы React (или другой фронтенд) на другом порту мог обращаться.
     */
    private void setCorsHeaders(HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, DELETE, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    /**
     * Перевіряє коректність fileId
     */
    private boolean isValidFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            LOGGER.warning("❌ fileId порожній або відсутній!");
            return false;
        }

        int lastDotIndex = fileId.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileId.length() - 1) {
            LOGGER.warning("❌ fileId без коректного розширення: " + fileId);
            return false;
        }

        String ext = fileId.substring(lastDotIndex).toLowerCase();

        if (BLACKLISTED_EXTENSIONS.contains(ext)) {
            LOGGER.warning("❌ Заборонене розширення: " + ext);
            return false;
        }

        return true;
    }

    /**
     * Визначає MIME-тип файлу за розширенням
     */
    private String getMimeType(String fileId) {
        String ext = fileId.substring(fileId.lastIndexOf(".")).toLowerCase();
        return mimeByExtension(ext);
    }

    /**
     * Повертає MIME-тип файлу за розширенням
     */
    private String mimeByExtension(String ext) {
        switch (ext) {
            case ".jpg":
            case ".jpeg": return "image/jpeg";
            case ".png":  return "image/png";
            case ".gif":  return "image/gif";
            case ".webp": return "image/webp";
            case ".bmp":  return "image/bmp";
            case ".svg":  return "image/svg+xml";

            case ".mp4":  return "video/mp4";
            case ".avi":  return "video/x-msvideo";
            case ".mov":  return "video/quicktime";
            case ".mkv":  return "video/x-matroska";

            case ".mp3":  return "audio/mpeg";
            case ".wav":  return "audio/wav";
            case ".ogg":  return "audio/ogg";
            case ".flac": return "audio/flac";

            case ".pdf":  return "application/pdf";
            case ".txt":  return "text/plain";
            case ".html": return "text/html";
            case ".css":  return "text/css";
            case ".js":   return "application/javascript";
            case ".json": return "application/json";
            case ".xml":  return "application/xml";

            case ".zip":  return "application/zip";
            case ".rar":  return "application/x-rar-compressed";
            case ".7z":   return "application/x-7z-compressed";
            case ".tar":  return "application/x-tar";
            case ".gz":   return "application/gzip";

            default:
                LOGGER.warning("⚠️ Невідомий MIME-тип: " + ext + ". Використано application/octet-stream");
                return "application/octet-stream";
        }
    }

    /**
     * Витягує fileId з запиту
     */
    private String extractFileId(HttpServletRequest req) {
        String fileId = req.getPathInfo();
        if (fileId != null && fileId.startsWith("/")) {
            fileId = fileId.substring(1);
        }
        return fileId;
    }
}

/*
=========================================


🔹 **Основне призначення:**
Цей сервлет обробляє HTTP-запити для отримання файлів зі сховища (`StorageService`).
Користувач може звернутися за адресою `/storage/{fileId}`, щоб отримати збережений файл.

🔍 **Ключові функції:**
✅ Отримує `fileId` із запиту (`PathInfo`)
✅ Перевіряє коректність `fileId` (не пустий, має розширення)
✅ Визначає `MIME-тип` файлу та надсилає його користувачеві
✅ Логування процесу: успішний запит, відсутність файлу, некоректний запит

⚠ **Обробка помилок:**
❌ Якщо `fileId` не переданий → `400 Bad Request`
❌ Якщо файл не знайдено → `404 Not Found`
❌ Якщо розширення файлу невідоме → використовується `application/octet-stream`

🔧 **Додатково:**
🔹 Функція `mimeByExtension()` містить список найпоширеніших розширень файлів
🔹 Функція `getMimeType()` виконує перевірку наявності розширення у файлі

📌 **Приклад запиту:**
- `http://localhost:8081/Java-Web-221/storage/example.jpg` – відкриє файл `example.jpg`
- `http://localhost:8081/Java-Web-221/storage/unknown` – поверне `application/octet-stream`, якщо немає розширення
*/
