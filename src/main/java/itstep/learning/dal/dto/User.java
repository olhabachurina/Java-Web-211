package itstep.learning.dal.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class User {

    private long id;                          // ID из базы, long
    private String name;
    private String login;                     // добавляем login
    private String city;
    private String address;
    private LocalDate birthdate;              // дата рождения
    private LocalDateTime registrationDate;   // дата регистрации
    private String role;                      // роль (из users_access.role_id)

    private boolean isEmailConfirmed;
    private String emailConfirmationToken;

    private List<String> emails;              // список email-ов
    private List<String> phones;              // список телефонов

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

    public LocalDate getBirthdate() {
        return birthdate;
    }
    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }
    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEmailConfirmed() {
        return isEmailConfirmed;
    }
    public void setEmailConfirmed(boolean emailConfirmed) {
        isEmailConfirmed = emailConfirmed;
    }

    public String getEmailConfirmationToken() {
        return emailConfirmationToken;
    }
    public void setEmailConfirmationToken(String emailConfirmationToken) {
        this.emailConfirmationToken = emailConfirmationToken;
    }

    public List<String> getEmails() {
        return emails;
    }
    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getPhones() {
        return phones;
    }
    public void setPhones(List<String> phones) {
        this.phones = phones;
    }

    // toString для логов
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", login='" + login + '\'' +
                ", city='" + city + '\'' +
                ", address='" + address + '\'' +
                ", birthdate=" + birthdate +
                ", registrationDate=" + registrationDate +
                ", role='" + role + '\'' +
                ", isEmailConfirmed=" + isEmailConfirmed +
                ", emails=" + emails +
                ", phones=" + phones +
                '}';
    }
}