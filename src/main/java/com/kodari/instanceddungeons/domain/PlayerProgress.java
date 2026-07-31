package com.kodari.instanceddungeons.domain;

import java.util.Map;
import java.util.UUID;

public record PlayerProgress(UUID playerId, UUID instanceId, String state,
                             Map<String, Object> data, long updatedAt) {
    public PlayerProgress {
        if (playerId == null || instanceId == null || state == null || state.isBlank() || data == null) {
            throw new IllegalArgumentException("Player progress contains invalid values.");
        }
        data = Map.copyOf(data);
    }
}