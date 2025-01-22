package itstep.learning.servlets;

import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
@WebServlet("/time")
public class TimeServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Встановлюємо тип контенту у відповіді: JSON з кодуванням UTF-8
        resp.setContentType("application/json;charset=UTF-8");

        // Отримуємо поточний час у мітці часу (timestamp)
        long timestamp = Instant.now().toEpochMilli();

        // Отримуємо поточний час у зоні за замовчуванням
        ZonedDateTime now = ZonedDateTime.now();

        // Форматуємо час у форматі ISO 8601
        String isoTime = now.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);

        // Форматуємо час  (дата і час)
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm:ss (zzz)", Locale.forLanguageTag("uk"));
        String formattedTime = now.format(customFormatter);

        // Формуємо JSON-відповідь
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", timestamp); // Додаємо мітку часу
        response.put("isoTime", isoTime); // Додаємо час у форматі ISO
        response.put("formattedTime", formattedTime); // Додаємо відформатований час

        // Відправляємо JSON
        resp.getWriter().print(gson.toJson(response));
    }
    }



