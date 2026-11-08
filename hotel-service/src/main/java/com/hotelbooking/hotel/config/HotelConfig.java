package com.hotelbooking.hotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "hotel.service")
public class HotelConfig {
    private boolean roomAvailabilityCacheEnabled = true;
    private int cacheExpirationMinutes = 15;
    private int maxRetryAttempts = 3;
    private long baseDelayMillis = 1000;

    public boolean isRoomAvailabilityCacheEnabled() {
        return roomAvailabilityCacheEnabled;
    }

    public void setRoomAvailabilityCacheEnabled(boolean roomAvailabilityCacheEnabled) {
        this.roomAvailabilityCacheEnabled = roomAvailabilityCacheEnabled;
    }

    public int getCacheExpirationMinutes() {
        return cacheExpirationMinutes;
    }

    public void setCacheExpirationMinutes(int cacheExpirationMinutes) {
        this.cacheExpirationMinutes = cacheExpirationMinutes;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public long getBaseDelayMillis() {
        return baseDelayMillis;
    }

    public void setBaseDelayMillis(long baseDelayMillis) {
        this.baseDelayMillis = baseDelayMillis;
    }
}