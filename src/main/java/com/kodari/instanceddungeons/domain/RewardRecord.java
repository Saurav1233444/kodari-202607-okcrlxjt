package com.kodari.instanceddungeons.domain;

import java.util.UUID;

public record RewardRecord(UUID id, UUID instanceId, UUID playerId, String type,
                          String payload, long createdAt) {
    public RewardRecord {
        if (id == null || instanceId == null || playerId == null || type == null || type.isBlank()
                || payload == null) {
            throw new IllegalArgumentException("Reward record contains invalid values.");
        }
    }
}