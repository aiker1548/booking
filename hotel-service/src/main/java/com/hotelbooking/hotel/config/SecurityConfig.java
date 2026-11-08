package com.hotelbooking.hotel.config;

import com.hotelbooking.hotel.util.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            @Value("${INTERNAL_API_SECRET:}") String internalApiSecret
    ) throws Exception {

        InternalApiKeyFilter internalApiKeyFilter =
                new InternalApiKeyFilter(internalApiSecret);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Internal Saga endpoints: authorization is handled by API key filter
                        .requestMatchers("/internal/**").permitAll()

                        // Health checks
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Hotel and room management: ADMIN only
                        .requestMatchers(HttpMethod.POST, "/hotels/**", "/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/hotels/**", "/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/hotels/**", "/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/hotels/**", "/rooms/**").hasRole("ADMIN")

                        // Reading hotel and room data: authenticated users
                        .requestMatchers(HttpMethod.GET, "/hotels/**", "/rooms/**")
                        .hasAnyRole("USER", "ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterBefore(
                        internalApiKeyFilter,
                        JwtAuthFilter.class
                )
                .build();
    }
}