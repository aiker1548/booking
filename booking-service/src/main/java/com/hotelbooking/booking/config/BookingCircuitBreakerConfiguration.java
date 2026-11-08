package com.hotelbooking.booking.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class BookingCircuitBreakerConfiguration {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
                .failureRateThreshold(50.0f)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50.0f)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .maxWaitDurationInHalfOpenState(Duration.ofSeconds(10))
                .slidingWindowSize(10)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .recordExceptions(
                        org.springframework.web.client.HttpServerErrorException.class,
                        org.springframework.web.client.ResourceAccessException.class,
                        java.net.ConnectException.class,
                        java.net.SocketTimeoutException.class,
                        java.io.IOException.class
                )
                .ignoreExceptions(
                        org.springframework.web.client.HttpClientErrorException.class
                );
        CircuitBreakerConfig config = builder.build();
        return CircuitBreakerRegistry.of(config);
    }
}