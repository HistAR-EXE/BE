package com.histar.be.auth.service;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.LogoutRequest;
import com.histar.be.auth.dto.RefreshTokenRequest;
import com.histar.be.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
