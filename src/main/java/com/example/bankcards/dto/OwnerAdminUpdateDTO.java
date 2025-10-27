package com.example.bankcards.dto;

import com.example.bankcards.util.annotation.ValidDateOfBirth;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class OwnerAdminUpdateDTO {
    @Size(min = 2, max = 100, message = "First name length must be between 2 and 100 characters.")
    private String firstName;

    @Size(min = 2, max = 100, message = "Last name length must be between 2 and 100 characters.")
    private String lastName;

    @ValidDateOfBirth
    private LocalDate dateOfBirth;

    public OwnerAdminUpdateDTO() {
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
}
