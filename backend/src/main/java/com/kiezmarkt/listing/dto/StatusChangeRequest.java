package com.kiezmarkt.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /listings/{id}/status}.
 */
public class StatusChangeRequest {

    @NotNull
    private StatusChangeTarget status;

    @Size(max = 120)
    private String reason;

    public StatusChangeTarget getStatus() {
        return status;
    }

    public void setStatus(StatusChangeTarget status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
