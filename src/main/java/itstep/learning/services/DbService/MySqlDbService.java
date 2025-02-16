package itstep.learning.services.DbService;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static itstep.learning.rest.RestResponse.gson;


@Singleton
public class MySqlDbService implements DbService {
    private Connection connection;

    @Override
    public Connection getConnection() {
        if (connection == null || isConnectionClosed()) {
            try {
                String url = "jdbc:mysql://localhost:3306/java221"
                        + "?useSSL=false"
                        + "&serverTimezone=UTC"
                        + "&useUnicode=true"
                        + "&characterEncoding=UTF-8";

                String user = "user221"; // Проверьте правильность имени пользователя
                String password = "pass221"; // Проверьте правильность пароля

                // Установка драйвера и соединения
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, password);

                System.out.println("✅ Соединение с базой данных установлено успешно.");
            } catch (ClassNotFoundException ex) {
                System.err.println("❌ JDBC-драйвер не найден: " + ex.getMessage());
            } catch (SQLException ex) {
                System.err.println("❌ Ошибка подключения к базе данных: " + ex.getMessage());
            }
        }
        return connection;
    }

    private boolean isConnectionClosed() {
        try {
            return connection == null || connection.isClosed();
        } catch (SQLException e) {
            System.err.println("❌ Ошибка проверки соединения: " + e.getMessage());
            return true;
        }
    }
    public <T> T fromBody(HttpServletRequest req, Class<T> classOfT) throws IOException {
        String charsetName = req.getCharacterEncoding();

        //  Если кодировка не указана, устанавливаем UTF-8
        if (charsetName == null) {
            charsetName = StandardCharsets.UTF_8.name();
        }

        //  Читаем тело запроса и конвертируем в строку с корректной кодировкой
        String json = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        //  Десериализуем JSON в объект нужного класса
        return gson.fromJson(json, classOfT);
    }
}