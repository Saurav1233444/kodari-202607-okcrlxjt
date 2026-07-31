package com.kodari.instanceddungeons.domain;

import java.util.Map;
import java.util.UUID;

public record DungeonDefinition(UUID id, String key, String displayName, String templateWorld,
                                Map<String, Object> data, long updatedAt) {
    public DungeonDefinition {
        if (id == null || key == null || key.isBlank() || displayName == null || displayName.isBlank()
                || templateWorld == null || templateWorld.isBlank() || data == null) {
            throw new IllegalArgumentException("Dungeon definition contains invalid values.");
        }
        data = Map.copyOf(data);
    }
}