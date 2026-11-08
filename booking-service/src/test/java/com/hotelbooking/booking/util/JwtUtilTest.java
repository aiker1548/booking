package com.hotelbooking.booking.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Use reflection to set the private fields
        try {
            java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, "test-secret-key-very-long-and-secure");

            java.lang.reflect.Field expirationField = JwtUtil.class.getDeclaredField("expirationMs");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, 3600000L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil for testing", e);
        }
    }

    @Test
    void generateToken_ShouldContainUsernameAndRole() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "testuser", "USER");

        assertNotNull(token);
        assertEquals("testuser", jwtUtil.getUsername(token));
        assertEquals("USER", jwtUtil.getRole(token));
        assertEquals(userId, jwtUtil.getUserId(token));
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "testuser", "USER");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidToken() {
        assertFalse(jwtUtil.validateToken("invalid-token"));
    }

    @Test
    void validateToken_ShouldReturnFalseForExpiredToken() {
        JwtUtil expiredUtil = new JwtUtil();
        try {
            java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(expiredUtil, "test-secret-key-very-long-and-secure");

            java.lang.reflect.Field expirationField = JwtUtil.class.getDeclaredField("expirationMs");
            expirationField.setAccessible(true);
            expirationField.set(expiredUtil, -1000L);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JwtUtil for testing", e);
        }
        String token = expiredUtil.generateToken(UUID.randomUUID(), "testuser", "USER");
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void getRole_ShouldReturnCorrectRole() {
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateToken(userId, "admin", "ADMIN");
        assertEquals("ADMIN", jwtUtil.getRole(token));
    }
}
