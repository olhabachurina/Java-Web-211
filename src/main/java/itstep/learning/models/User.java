
package itstep.learning.models;
import org.mindrot.jbcrypt.BCrypt;
import java.util.List;

public class User {
    private long id;
    private String name;
    private String login;
    private List<String> emails; // Поле для списка email
    private List<String> phones; // Поле для списка телефонов
    private String city;
    private String address;
    private String birthdate;
    private String password;

    // Геттеры и сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public List<String> getEmails() { // Геттер для emails
        return emails;
    }

    public void setEmails(List<String> emails) { // Сеттер для emails
        this.emails = emails;
    }

    public List<String> getPhones() { // Геттер для phones
        return phones;
    }

    public void setPhones(List<String> phones) { // Сеттер для phones
        this.phones = phones;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
