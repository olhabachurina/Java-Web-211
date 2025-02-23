
package itstep.learning.models;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class User {
    private long id;
    private String name;
    private String login;
    private List<String> emails = new ArrayList<>();
    private List<String> phones = new ArrayList<>();
    private String city;
    private String address;
    private String birthdate;
    private String password;
    private Timestamp registrationDate;
    private boolean isEmailConfirmed;
    private String emailConfirmationToken;
    private Timestamp tokenCreatedAt;
    private String role;
    private String avatarPath;
    public User() {}

    // ✅ Конструктор для упрощенного создания объекта
    public User(Long id, String name, String login) {
        this.id = id;
        this.name = name;
        this.login = login;
    }

    public User(Long id, String name, String login, String city, String address, String birthdate, String role) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.city = city;
        this.address = address;
        this.birthdate = birthdate;
        this.role = role;
    }
    public User(long id, String name, String login, String city, String address,
                String birthdate, String roleId, String emails, List<String> phones) {
        this.id = id;
        this.name = name;
        this.login = login;
        this.city = city;
        this.address = address;
        this.birthdate = birthdate;
        this.role = roleId;
        this.emails = Arrays.asList(emails.split(","));
        this.phones = phones;
    }
    // ✅ Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public List<String> getEmails() { return emails; }
    public void setEmails(List<String> emails) { this.emails = (emails != null) ? emails : new ArrayList<>(); }

    public List<String> getPhones() { return phones; }
    public void setPhones(List<String> phones) { this.phones = (phones != null) ? phones : new ArrayList<>(); }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBirthdate() { return birthdate; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Timestamp getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Timestamp registrationDate) { this.registrationDate = registrationDate; }

    public boolean isEmailConfirmed() { return isEmailConfirmed; }
    public void setEmailConfirmed(boolean emailConfirmed) { isEmailConfirmed = emailConfirmed; }

    public String getEmailConfirmationToken() { return emailConfirmationToken; }
    public void setEmailConfirmationToken(String emailConfirmationToken) { this.emailConfirmationToken = emailConfirmationToken; }

    public Timestamp getTokenCreatedAt() { return tokenCreatedAt; }
    public void setTokenCreatedAt(Timestamp tokenCreatedAt) { this.tokenCreatedAt = tokenCreatedAt; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    /**
     * ✅ Создает объект `User` из `ResultSet`, учитывая возможные `NULL` значения.
     * @param rs - результат SQL-запроса
     * @return User
     * @throws SQLException
     */
    public static User fromResultSet(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setName(getSafeString(rs, "name"));
        user.setLogin(getSafeString(rs, "login"));
        user.setCity(getSafeString(rs, "city"));
        user.setAddress(getSafeString(rs, "address"));
        user.setBirthdate(getSafeString(rs, "birthdate"));
        user.setPassword(getSafeString(rs, "password"));
        user.setEmailConfirmed(rs.getBoolean("is_email_confirmed"));
        user.setEmailConfirmationToken(getSafeString(rs, "email_confirmation_token"));

        // ✅ Обрабатываем `NULL` значения для `timestamp`
        user.setRegistrationDate(getSafeTimestamp(rs, "registration_date").orElse(null));
        user.setTokenCreatedAt(getSafeTimestamp(rs, "token_created_at").orElse(null));

        // ✅ Загружаем `emails` и `phones`
        user.setEmails(getSafeStringList(rs, "emails"));
        user.setPhones(getSafeStringList(rs, "phones"));

        return user;
    }

    /**
     * ✅ Безопасное получение `String`, обрабатывая `NULL`.
     */
    private static String getSafeString(ResultSet rs, String column) {
        try {
            return Optional.ofNullable(rs.getString(column)).orElse("");
        } catch (SQLException e) {
            System.err.println("Ошибка: Колонка '" + column + "' не найдена в ResultSet!");
            return "";
        }
    }

    /**
     * ✅ Безопасно получает `Timestamp`, обрабатывая `NULL`
     * @param rs - `ResultSet`
     * @param column - имя колонки
     * @return `Optional<Timestamp>` (чтобы избежать `null`)
     * @throws SQLException
     */
    private static Optional<Timestamp> getSafeTimestamp(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return (rs.wasNull()) ? Optional.empty() : Optional.of(value);
    }

    /**
     * ✅ Получает список строк из `ResultSet`, разделенных `;`
     * @param rs - `ResultSet`
     * @param column - имя колонки
     * @return `List<String>` (пустой список, если колонка не существует или `NULL`)
     */
    private static List<String> getSafeStringList(ResultSet rs, String column) {
        try {
            String value = rs.getString(column);
            return (value != null && !value.isEmpty()) ? Arrays.asList(value.split("; ")) : new ArrayList<>();
        } catch (SQLException e) {
            return new ArrayList<>();
        }
    }

    /**
     * ✅ `toString()`, чтобы удобно выводить объект
     */
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", login='" + login + '\'' +
                ", city='" + city + '\'' +
                ", address='" + address + '\'' +
                ", birthdate='" + birthdate + '\'' +
                ", role='" + role + '\'' +
                ", emails=" + emails +
                ", phones=" + phones +
                '}';
    }

    /**
     * ✅ `equals()` и `hashCode()`, чтобы сравнивать пользователей корректно
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id == user.id && Objects.equals(login, user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, login);
    }

    public void setAvatarPath(String s) {
        this.avatarPath = s;
    }

    public String getAvatarPath() {
        return avatarPath;
    }
}
