package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.services.random.DateTimeService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
@Singleton
@WebServlet("/time")
public class TimeServlet extends HttpServlet {
    private final Gson gson = new Gson();
    private final DateTimeService dateTimeService;

    // Конструктор для Guice
    @Inject
    public TimeServlet(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    // Конструктор по умолчанию (обязателен для Tomcat)
    public TimeServlet() {
        this.dateTimeService = null; // Guice будет использовать свой конструктор
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");

        // Проверка на null, если Guice не инициализировал сервис
        if (dateTimeService == null) {
            resp.getWriter().write("{\"error\": \"Service not initialized\"}");
            return;
        }

        long timestamp = dateTimeService.getTimestamp();
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", timestamp);

        resp.getWriter().write(gson.toJson(response));
    }
}