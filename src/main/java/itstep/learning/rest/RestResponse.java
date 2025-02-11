package itstep.learning.rest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Map;

public class RestResponse {
    private int status;
    private String message;
    private String resourceUrl;
    private long cacheTime; // Кеширование в секундах
    private Map<String, String> meta;
    private Object data;

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Конструктор по умолчанию
    public RestResponse() {
        this.status = 200;
        this.message = "OK";
        this.cacheTime = 0;
    }

    // Конструктор с параметрами
    public RestResponse(int status, String message, String resourceUrl, long cacheTime, Map<String, String> meta, Object data) {
        this.status = status;
        this.message = message;
        this.resourceUrl = resourceUrl;
        this.cacheTime = cacheTime;
        this.meta = meta;
        this.data = data;
    }

    // Геттеры и сеттеры
    public int getStatus() {
        return status;
    }

    public RestResponse setStatus(int status) {
        this.status = status;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public RestResponse setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getResourceUrl() {
        return resourceUrl;
    }

    public RestResponse setResourceUrl(String resourceUrl) {
        this.resourceUrl = resourceUrl;
        return this;
    }

    public long getCacheTime() {
        return cacheTime;
    }

    public RestResponse setCacheTime(long cacheTime) {
        this.cacheTime = cacheTime;
        return this;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public RestResponse setMeta(Map<String, String> meta) {
        this.meta = meta;
        return this;
    }

    public Object getData() {
        return data;
    }

    public RestResponse setData(Object data) {
        this.data = data;
        return this;
    }

    // Преобразование в JSON
    public String toJson() {
        return gson.toJson(this);
    }

    // Метод для быстрой сборки ответа
    public static RestResponse build(int status, String message, String resourceUrl, long cacheTime, Map<String, String> meta, Object data) {
        return new RestResponse(status, message, resourceUrl, cacheTime, meta, data);
    }
}
