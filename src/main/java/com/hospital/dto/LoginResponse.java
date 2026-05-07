package com.hospital.dto;

public class LoginResponse {
    private String token;
    private String email;
    private String role;
    private String fullName;

    public LoginResponse() {}

    public LoginResponse(String token, String email, String role, String fullName) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.fullName = fullName;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
}
