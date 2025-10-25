package com.example.bankcards.dto;

import com.example.bankcards.entity.Role;
import com.example.bankcards.util.annotation.ValidDateOfBirth;
import com.example.bankcards.util.annotation.ValidPassword;
import com.example.bankcards.util.annotation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class OwnerRequestDTO {
    @NotBlank(message = "First name can't be empty")
    @Size(min = 2, max = 100, message = "First name length must be between 2 and 100 characters.")
    private String firstName;

    @NotBlank(message = "Last name can't be empty")
    @Size(min = 2, max = 100, message = "Last name length must be between 2 and 100 characters.")
    private String lastName;

    @ValidDateOfBirth
    private LocalDate dateOfBirth;

    @NotBlank(message = "email can't be empty")
    @Email(message = "Email should be valid!")
    @Size(max = 100, message = "Email can contain a maximum of 100 characters")
    private String email;

    @ValidPhoneNumber
    private String phone;

    @NotBlank(message = "Password can't be empty!")
    @ValidPassword
    private String password;

    public OwnerRequestDTO() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
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
