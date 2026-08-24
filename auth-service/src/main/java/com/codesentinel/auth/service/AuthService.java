package com.codesentinel.auth.service;

import com.codesentinel.auth.dto.AuthResponse;
import com.codesentinel.auth.dto.LoginRequest;
import com.codesentinel.auth.dto.RegisterRequest;
import com.codesentinel.auth.entity.Role;
import com.codesentinel.auth.entity.User;
import com.codesentinel.auth.repository.UserRepository;
import com.codesentinel.common.exception.DuplicateResourceException;
import com.codesentinel.common.exception.UnauthorizedException;
import com.codesentinel.common.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public AuthResponse register(RegisterRequest request) {
        // 1. Check if email is already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        // 2. Build the user and hash the password
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Default role for new signups
                .build();

        // 3. Save to database
        userRepository.save(user);

        // 4. Generate JWT token (valid for 24 hours)
        String token = jwtUtils.generateToken(user.getEmail(), List.of(user.getRole().name()), 1000 * 60 * 60 * 24);

        // 5. Return the response
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        // 2. Verify password matches the hash
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        // 3. Generate new JWT token
        String token = jwtUtils.generateToken(user.getEmail(), List.of(user.getRole().name()), 1000 * 60 * 60 * 24);

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}