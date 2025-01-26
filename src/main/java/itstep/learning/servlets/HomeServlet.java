package itstep.learning.servlets;

import com.google.gson.Gson;
import itstep.learning.rest.RestResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    public final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String message;
        int status;
        String currentTime = null;
        String databases = null; // Для результату запиту "SHOW DATABASES"
        String connectionString = "jdbc:mysql://localhost:3306/Java221?useSSL=false&serverTimezone=UTC";

        resp.setContentType("application/json;charset=UTF-8");

        try {
            // Завантаження драйвера (за необхідності)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Підключення до бази даних
            try (Connection connection = DriverManager.getConnection(connectionString, "user221", "pass221")) {
                if (connection != null) {
                    // Отримуємо поточний час
                    String timeQuery = "SELECT CURRENT_TIMESTAMP";
                    try (Statement timeStatement = connection.createStatement();
                         ResultSet timeResultSet = timeStatement.executeQuery(timeQuery)) {
                        if (timeResultSet.next()) {
                            currentTime = timeResultSet.getString(1);
                        }
                    }

                    // Отримуємо бази даних
                    String dbQuery = "SHOW DATABASES";
                    try (Statement dbStatement = connection.createStatement();
                         ResultSet dbResultSet = dbStatement.executeQuery(dbQuery)) {
                        StringBuilder dbBuilder = new StringBuilder();
                        while (dbResultSet.next()) {
                            if (dbBuilder.length() > 0) {
                                dbBuilder.append(", "); // Додаємо кому між базами
                            }
                            dbBuilder.append(dbResultSet.getString(1)); // Отримуємо назву бази даних
                        }
                        databases = dbBuilder.toString();
                    }

                    message = "Query executed successfully.";
                    status = 200;
                } else {
                    message = "Connection is null.";
                    status = 500;
                }
            }
        } catch (ClassNotFoundException ex) {
            message = "JDBC Driver not found: " + ex.getMessage();
            status = 500;
        } catch (SQLException ex) {
            message = "Error: " + ex.getMessage();
            status = 500;
        }

        // Формуємо JSON-відповідь
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", status);
            response.put("message", message);
            response.put("currentTime", currentTime);
            response.put("databases", databases); // Додаємо бази даних до відповіді

            resp.getWriter().print(gson.toJson(response));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}