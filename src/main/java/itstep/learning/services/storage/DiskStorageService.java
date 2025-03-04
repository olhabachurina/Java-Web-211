package itstep.learning.services.storage;
import com.google.inject.Singleton;
import itstep.learning.services.config.JsonConfigService;
import jakarta.inject.Inject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

@Singleton
public class DiskStorageService implements StorageService {

    private static final Logger LOGGER = Logger.getLogger(DiskStorageService.class.getName());
    private final Path storagePath;

    @Inject
    public DiskStorageService(JsonConfigService configService) {
        // Отримуємо шлях до сховища з конфігурації
        this.storagePath = Paths.get(configService.getString("storage.path"));

        // Створюємо директорію, якщо вона відсутня
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            LOGGER.severe("Помилка при створенні каталогу сховища: " + e.getMessage());
            throw new RuntimeException("Не вдалося ініціалізувати DiskStorageService", e);
        }
    }

    @Override
    public String put(InputStream inputStream, String ext) throws IOException {
        if (ext == null || !ext.matches("\\.[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("Некоректне розширення файлу: " + ext);
        }

        String itemId = UUID.randomUUID().toString() + ext;
        Path filePath = storagePath.resolve(itemId);
        Path tempFilePath = storagePath.resolve(itemId + ".tmp");

        try (FileOutputStream writer = new FileOutputStream(tempFilePath.toFile())) {
            byte[] buffer = new byte[131072];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
            }
        }

        Files.move(tempFilePath, filePath, StandardCopyOption.ATOMIC_MOVE);
        LOGGER.info("Файл збережено: " + filePath);
        return itemId;
    }

    @Override
    public InputStream get(String itemId) throws IOException {
        if (itemId == null || itemId.contains("..")) {
            throw new IllegalArgumentException("Некоректне ім'я файлу: " + itemId);
        }

        Path filePath = storagePath.resolve(itemId);

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new FileNotFoundException("Файл не знайдено: " + filePath);
        }

        return new BufferedInputStream(new FileInputStream(filePath.toFile()));
    }
}