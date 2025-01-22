package itstep.learning.servlets;

import com.google.gson.Gson;
import itstep.learning.rest.RestResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/home") // Аннотація для визначення URL-адреси, за якою доступний цей сервлет ("/home")
public class HomeServlet extends HttpServlet {

    public final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // Встановлюємо тип контенту у відповіді: JSON
        resp.setContentType("application/json");

        try {

            resp.getWriter().print(
                    gson.toJson(new RestResponse()
                            .setStatus(200)
                            .setMessage("Ok")
                    )
            );
        } catch (Exception e) {
            // Обробляємо виключення
            e.printStackTrace();
        }
    }
}
