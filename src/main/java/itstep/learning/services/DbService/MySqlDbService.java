package itstep.learning.services.DbService;

import com.google.inject.Singleton;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Singleton
public class MySqlDbService implements DbService {
    private Connection connection; // Поле для хранения подключения

    @Override
    public Connection getConnection() {
        if (connection == null) { // Если подключение ещё не установлено
            try {
                // Настройки подключения
                String url = "jdbc:mysql://localhost:3306/Java221?useSSL=false&serverTimezone=UTC";
                String user = "user221";
                String password = "pass221";

                // Установка драйвера и соединения
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, user, password);

                System.out.println("Соединение с базой данных установлено успешно.");
            } catch (ClassNotFoundException ex) {
                System.err.println("JDBC-драйвер не найден: " + ex.getMessage());
            } catch (SQLException ex) {
                System.err.println("Ошибка подключения к базе данных: " + ex.getMessage());
            }
        }
        return connection; // Возвращаем соединение
    }
}