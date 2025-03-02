package itstep.learning.servlets;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import itstep.learning.rest.RestResponse;
import itstep.learning.services.random.RandomService;
import itstep.learning.services.random.RandomServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

@Singleton
@WebServlet("/random")
public class RandomServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(RandomServlet.class.getName());

    @Inject
    private RandomService randomService; // Инъекция зависимости через Google Guice

    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOGGER.info("GET-запит отримано: " + req.getQueryString());

        // Получаем параметры запроса
        String type = req.getParameter("type");
        String lengthParam = req.getParameter("length");

        if (type == null || type.isEmpty()) {
            sendErrorResponse(resp, 400, "Параметр 'type' відсутній");
            return;
        }

        int length = 10; // Значение по умолчанию
        if (lengthParam != null) {
            try {
                length = Integer.parseInt(lengthParam);
                if (length <= 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                sendErrorResponse(resp, 400, "Некоректне значення параметра 'length'");
                return;
            }
        }

        // Генерация строки
        String result;
        try {
            switch (type.toLowerCase()) {
                case "salt":
                    result = randomService.randomString(length);
                    break;
                case "filename":
                    result = randomService.randomFileName(length);
                    break;
                default:
                    sendErrorResponse(resp, 400, "Невідомий тип: " + type);
                    return;
            }
        } catch (Exception e) {
            sendErrorResponse(resp, 500, "Помилка генерації випадкового рядка");
            return;
        }

        // Формируем JSON-ответ
        RestResponse restResponse = new RestResponse()
                .setStatus(200)
                .setResourceUrl("GET /random")
                .setCacheTime(0)
                .setMeta(Map.of(
                        "dataType", "string",
                        "read", "GET /random",
                        "type", type,
                        "length", String.valueOf(length)
                ))
                .setData(result);

        sendJsonResponse(resp, 200, gson.toJson(restResponse));
    }

    // Отправка JSON-ответа
    private void sendJsonResponse(HttpServletResponse resp, int statusCode, String json) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(json);
    }

    // Отправка ошибки
    private void sendErrorResponse(HttpServletResponse resp, int statusCode, String message) throws IOException {
        sendJsonResponse(resp, statusCode, "{\"error\": \"" + message + "\"}");
    }
}