package itstep.learning.ioc;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@WebListener
public class DbConfigListener implements ServletContextListener {
    private Connection connection;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://127.0.0.1:3306/Java221?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true",
                    "user221",
                    "pass221"
            );
            sce.getServletContext().setAttribute("dbConnection", connection);
            System.out.println("✅ Подключение к БД установлено!");
        } catch (Exception e) {
            throw new RuntimeException("❌ Ошибка подключения к БД!", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("✅ Подключение к БД закрыто!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}