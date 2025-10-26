package com.example.bankcards.dto;

import com.example.bankcards.util.annotation.ValidPassword;
import com.example.bankcards.util.annotation.ValidPhoneNumber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class OwnerUpdateDTO {
    @ValidPassword
    private String password;

    @Email
    @Size(min = 5, max = 100)
    @Pattern(regexp = ".+@.+\\..+", message = "Email is invalid")
    private String email;

    @ValidPhoneNumber
    private String phone;

    public OwnerUpdateDTO() {
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
}
