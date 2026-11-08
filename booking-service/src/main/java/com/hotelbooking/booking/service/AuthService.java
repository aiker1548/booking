package com.hotelbooking.booking.service;

import com.hotelbooking.booking.dto.LoginRequest;
import com.hotelbooking.booking.dto.LoginResponse;
import com.hotelbooking.booking.dto.RegisterRequest;
import com.hotelbooking.booking.entity.User;
import com.hotelbooking.booking.repository.UserRepository;
import com.hotelbooking.booking.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        User user = new User(
                UUID.randomUUID(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                User.Role.USER
        );
        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        log.info("User {} logged in", user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getRole().name());
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }
}