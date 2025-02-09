package itstep.learning.services.random;

public interface RandomService {
    int randomInt();
    // Метод для генерации случайной строки заданной длины
    String randomString(int length);

    // Метод для генерации случайного имени файла заданной длины
    String randomFileName(int length);
}