package com.example.bankcards.dto;

import com.example.bankcards.util.annotation.ValidCode;

public class CodeRequestDTO {
    @ValidCode
    private String code;

    public CodeRequestDTO(String code) {
        this.code = code;
    }

    public CodeRequestDTO() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
