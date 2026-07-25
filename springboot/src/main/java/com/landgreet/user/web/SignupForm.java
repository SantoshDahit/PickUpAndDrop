package com.landgreet.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupForm {

    @NotBlank(message = "Please tell us your name.")
    @Size(max = 100, message = "Name is too long.")
    private String name;

    @NotBlank(message = "Please enter your email.")
    @Email(message = "That doesn't look like an email address.")
    @Size(max = 254, message = "Email is too long.")
    private String email;

    @Size(max = 30, message = "Phone number is too long.")
    private String phone;

    @NotBlank(message = "Please choose a password.")
    @Size(min = 6, max = 200, message = "Password needs at least 6 characters.")
    private String password;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
