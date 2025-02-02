package itstep.learning.rest;

import java.util.Map;

public class RestResponse {
    private int status;
    private String message;
    private String resourceUrl;
    private long cacheTime; // время в секундах

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
}
