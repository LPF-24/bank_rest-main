package com.example.bankcards.dto;

public class JWTResponse {
    private String accessToken;

    private Long id;

    private String email;

    public JWTResponse(String accessToken, Long id, String email) {
        this.accessToken = accessToken;
        this.id = id;
        this.email = email;
    }

    public JWTResponse() {
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
