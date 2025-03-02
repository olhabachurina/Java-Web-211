package itstep.learning.services.config;

public interface ConfigService {
    Object getValue (String path);
    String getString(String key);
    int getInt(String key);
    boolean getBoolean(String key);
}
