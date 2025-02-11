package itstep.learning.rest;

import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;


import java.io.IOException;

public class RestService {
    private final Gson gson = new Gson();

    public void sendResponse(HttpServletResponse resp, RestResponse restResponse) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setStatus(HttpServletResponse.SC_OK);

        // Преобразование ответа в JSON
        String jsonResponse = gson.toJson(restResponse);

        // Отправка ответа
        resp.getWriter().write(jsonResponse);
    }
}