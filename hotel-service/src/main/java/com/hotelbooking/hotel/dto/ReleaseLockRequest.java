package com.hotelbooking.hotel.dto;

import java.util.UUID;

public class ReleaseLockRequest {
    private UUID requestId;

    public UUID getRequestId() { return requestId; }
    public void setRequestId(UUID requestId) { this.requestId = requestId; }
}
