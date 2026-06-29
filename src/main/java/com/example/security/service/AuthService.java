package com.example.security.service;

import com.example.security.dto.request.LoginRequest;
import com.example.security.dto.request.RegisterRequest;
import com.example.security.dto.response.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    AuthResponse refreshToken(String refreshToken, HttpServletRequest httpRequest);
    void logout(String refreshToken, String username, HttpServletRequest httpRequest);
}
