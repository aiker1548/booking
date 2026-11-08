package com.hotelbooking.api.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("hotel-service", r -> r
                .path("/hotel-api/**")
                .uri("http://hotel-service:8081/"))
            .route("booking-service", r -> r
                .path("/booking-api/**")
                .uri("http://booking-service:8082/"))
            .build();
    }
}