package com.kodari.instanceddungeons.dungeon.definition;

import java.util.Map;
import java.util.Objects;

public record ObjectiveSpec(String id, ObjectiveType type, String description, String targetIdentifier,
                            int requiredAmount, RegionSpec targetRegion) {
    public ObjectiveSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(description, "description");
        if (requiredAmount <= 0) {
            requiredAmount = 1;
        }
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "type", type.name(),
                "description", description,
                "targetIdentifier", targetIdentifier != null ? targetIdentifier : "",
                "requiredAmount", requiredAmount,
                "targetRegion", targetRegion != null ? targetRegion.toMap() : Map.of()
        );
    }

    public static ObjectiveSpec fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.getOrDefault("id", "obj_1");
        String typeStr = (String) map.getOrDefault("type", "KILL_MOB");
        ObjectiveType type = ObjectiveType.valueOf(typeStr);
        String desc = (String) map.getOrDefault("description", "Objective");
        String target = (String) map.getOrDefault("targetIdentifier", "");
        int req = ((Number) map.getOrDefault("requiredAmount", 1)).intValue();
        @SuppressWarnings("unchecked")
        Map<String, Object> regMap = (Map<String, Object>) map.get("targetRegion");
        RegionSpec reg = RegionSpec.fromMap(regMap);
        return new ObjectiveSpec(id, type, desc, target, req, reg);
    }
}
