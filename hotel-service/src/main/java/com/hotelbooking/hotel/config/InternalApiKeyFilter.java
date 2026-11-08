package com.hotelbooking.hotel.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Internal-Secret";

    private final String expectedSecret;

    public InternalApiKeyFilter(String expectedSecret) {
        this.expectedSecret = expectedSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.equals("/internal") && !path.startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String providedSecret = request.getHeader(HEADER_NAME);

        if (expectedSecret == null
                || expectedSecret.isBlank()
                || providedSecret == null
                || !MessageDigest.isEqual(
                        expectedSecret.getBytes(StandardCharsets.UTF_8),
                        providedSecret.getBytes(StandardCharsets.UTF_8)
                )) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}