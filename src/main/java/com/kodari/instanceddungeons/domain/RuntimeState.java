package com.kodari.instanceddungeons.domain;

import java.util.Map;
import java.util.UUID;

public record RuntimeState(UUID instanceId, String state, Map<String, Object> data, long updatedAt) {
    public RuntimeState {
        if (instanceId == null || state == null || state.isBlank() || data == null) {
            throw new IllegalArgumentException("Runtime state contains invalid values.");
        }
        data = Map.copyOf(data);
    }
}