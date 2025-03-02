package itstep.learning.services.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реалізація сервісу конфігурації, що завантажує appsettings.json з ресурсів
 * та дозволяє отримувати значення за шляхом (наприклад, "db.host").
 */
@Singleton
public class JsonConfigService implements ConfigService {
    private final Logger logger;
    private JsonObject config;

    @Inject
    public JsonConfigService(Logger logger) {
        this.logger = logger;
        init();
    }

    private void init() {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("appsettings.json")) {
            if (stream == null) {
                logger.severe("❌ Файл appsettings.json не найден!");
                throw new RuntimeException("Файл appsettings.json отсутствует!");
            }
            // Улучшение: можно добавить Reader в блок try-with-resources для гарантированного закрытия
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                config = gson.fromJson(reader, JsonObject.class);
            }
            logger.info("✅ JsonConfigService: конфигурация загружена успешно.");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "❌ Ошибка чтения appsettings.json", ex);
            throw new RuntimeException("Ошибка чтения appsettings.json", ex);
        }
    }

    @Override
    public Object getValue(String path) {
        logger.info("🔹 JsonConfigService: запрашивается значение для ключа " + path);
        String[] keys = path.split("\\.");
        JsonElement current = config;

        for (String key : keys) {
            if (current.isJsonObject() && current.getAsJsonObject().has(key)) {
                current = current.getAsJsonObject().get(key);
            } else {
                logger.severe("❌ JsonConfigService: Ключ '" + key + "' не найден в конфигурации (путь: " + path + ")");
                throw new RuntimeException("Ключ '" + key + "' не найден в конфигурации (путь: " + path + ")");
            }
        }
        return current;
    }

    @Override
    public int getInt(String key) {
        return ((JsonElement) getValue(key)).getAsInt();
    }

    @Override
    public boolean getBoolean(String key) {
        return ((JsonElement) getValue(key)).getAsBoolean();
    }

    @Override
    public String getString(String key) {
        return ((JsonElement) getValue(key)).getAsString();
    }
}