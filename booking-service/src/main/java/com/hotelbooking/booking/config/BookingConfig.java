package com.hotelbooking.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "booking.service")
public class BookingConfig {
    private boolean sagaEnabled = true;
    private long sagaTimeoutMillis = 300000;
    private int sagaRetryAttempts = 3;
    private long sagaRetryDelayMillis = 2000;

    public boolean isSagaEnabled() {
        return sagaEnabled;
    }

    public void setSagaEnabled(boolean sagaEnabled) {
        this.sagaEnabled = sagaEnabled;
    }

    public long getSagaTimeoutMillis() {
        return sagaTimeoutMillis;
    }

    public void setSagaTimeoutMillis(long sagaTimeoutMillis) {
        this.sagaTimeoutMillis = sagaTimeoutMillis;
    }

    public int getSagaRetryAttempts() {
        return sagaRetryAttempts;
    }

    public void setSagaRetryAttempts(int sagaRetryAttempts) {
        this.sagaRetryAttempts = sagaRetryAttempts;
    }

    public long getSagaRetryDelayMillis() {
        return sagaRetryDelayMillis;
    }

    public void setSagaRetryDelayMillis(long sagaRetryDelayMillis) {
        this.sagaRetryDelayMillis = sagaRetryDelayMillis;
    }
}