
package itstep.learning.models;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    // ✅ Пустой конструктор (необходим для fromResultSet)
    public User() {}

    // ✅ Конструктор для упрощенного создания объекта
    public User(Long id, String name, String login) {
        this.id = id;
        this.name = name;
        this.login = login;
    }

    // ✅ Геттеры и сеттеры
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public List<String> getEmails() { return emails; }
    public void setEmails(List<String> emails) { this.emails = emails != null ? emails : new ArrayList<>(); }

    public List<String> getPhones() { return phones; }
    public void setPhones(List<String> phones) { this.phones = phones != null ? phones : new ArrayList<>(); }

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


    public static User fromResultSet(ResultSet rs) throws SQLException {
        User user = new User();

        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setLogin(rs.getString("login"));
        user.setCity(rs.getString("city"));
        user.setAddress(rs.getString("address"));
        user.setBirthdate(rs.getString("birthdate"));
        user.setPassword(rs.getString("password"));
        user.setEmailConfirmed(rs.getBoolean("is_email_confirmed"));
        user.setEmailConfirmationToken(rs.getString("email_confirmation_token"));

        // ✅ Безпечна обробка `timestamp` (NULL значення)
        Timestamp registrationDate = rs.getTimestamp("registration_date");
        user.setRegistrationDate(rs.wasNull() ? null : registrationDate);

        Timestamp tokenCreatedAt = rs.getTimestamp("token_created_at");
        user.setTokenCreatedAt(rs.wasNull() ? null : tokenCreatedAt);

        // ✅ Захист від помилки, якщо колонка `emails` не існує
        try {
            String emailsStr = rs.getString("emails");
            user.setEmails(emailsStr != null ? Arrays.asList(emailsStr.split(",")) : new ArrayList<>());
        } catch (SQLException e) {
            user.setEmails(new ArrayList<>());
        }

        try {
            String phonesStr = rs.getString("phones");
            user.setPhones(phonesStr != null ? Arrays.asList(phonesStr.split(",")) : new ArrayList<>());
        } catch (SQLException e) {
            user.setPhones(new ArrayList<>());
        }

        return user;
    }

}