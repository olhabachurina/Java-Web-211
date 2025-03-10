package itstep.learning.servlets;

import itstep.learning.services.storage.StorageService;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Singleton;

@Singleton
public class StorageServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(StorageServlet.class.getName());
    private final StorageService storageService;

    // Чорний список небезпечних розширень
    private static final Set<String> BLACKLISTED_EXTENSIONS = Set.of(".exe", ".php", ".py", ".cgi", ".sh", ".bat", ".cmd", ".jsp", ".asp", ".aspx");

    @Inject
    public StorageServlet(StorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");

        // Отримуємо `fileId` з `PathInfo` (наприклад, "/example.jpg")
        String fileId = req.getPathInfo();
        if (fileId != null && fileId.startsWith("/")) {
            fileId = fileId.substring(1); // Видаляємо "/"
        }

        // 🔍 Валідація fileId
        if (!isValidFileId(fileId)) {
            LOGGER.warning("❌ Некоректний fileId: " + fileId);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "❌ Некоректний fileId або заборонене розширення");
            return;
        }

        LOGGER.info("📥 Отримано запит на відкриття файлу: " + fileId);

        // Отримуємо файл зі сховища
        try (InputStream fileStream = storageService.get(fileId)) {
            // Визначаємо MIME-тип файлу
            String mimeType = getMimeType(fileId);
            resp.setContentType(mimeType);

            // Встановлюємо заголовки
            resp.setHeader("Content-Disposition", "inline; filename=\"" + fileId + "\"");

            // Копіюємо файл у відповідь
            try (OutputStream out = resp.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fileStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            LOGGER.info("✅ Файл успішно відкрито: " + fileId + " (MIME: " + mimeType + ")");
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "❌ Файл не знайдено: " + fileId, e);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "❌ Файл не знайдено");
        }
    }

    /**
     * Перевіряє коректність `fileId`
     */
    private boolean isValidFileId(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            LOGGER.warning("❌ fileId порожній!");
            return false;
        }

        // Перевіряємо наявність точки (".") та розширення
        int lastDotIndex = fileId.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileId.length() - 1) {
            LOGGER.warning("❌ fileId не містить коректного розширення: " + fileId);
            return false;
        }

        // Отримуємо розширення
        String ext = fileId.substring(lastDotIndex).toLowerCase();

        // Перевіряємо чорний список розширень
        if (BLACKLISTED_EXTENSIONS.contains(ext)) {
            LOGGER.warning("❌ Заборонене розширення файлу: " + ext);
            return false;
        }

        return true;
    }

    /**
     * Визначає MIME-тип файлу за його розширенням
     */
    private String getMimeType(String fileId) {
        String ext = fileId.substring(fileId.lastIndexOf(".")).toLowerCase();
        return mimeByExtension(ext);
    }

    /**
     * Повертає MIME-тип для розширення файлу
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
                LOGGER.warning("⚠️ Невідомий MIME-тип для: " + ext + ", використано application/octet-stream");
                return "application/octet-stream"; // За замовчуванням — бінарні дані
        }
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
