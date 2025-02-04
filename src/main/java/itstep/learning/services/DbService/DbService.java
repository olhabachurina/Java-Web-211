package itstep.learning.services.DbService;

import java.sql.Connection;

public interface DbService {
    Connection getConnection(); // Метод для получения подключения
}