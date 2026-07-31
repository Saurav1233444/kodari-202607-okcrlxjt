package com.kodari.instanceddungeons.domain;

import java.util.UUID;

public record InstanceRecord(UUID id, UUID dungeonId, String worldName, String state,
                             long createdAt, Long completedAt) {
    public InstanceRecord {
        if (id == null || dungeonId == null || worldName == null || worldName.isBlank()
                || state == null || state.isBlank()) {
            throw new IllegalArgumentException("Instance record contains invalid values.");
        }
    }
}