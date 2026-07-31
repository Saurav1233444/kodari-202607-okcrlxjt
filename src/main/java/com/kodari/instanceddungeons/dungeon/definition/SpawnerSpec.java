package com.kodari.instanceddungeons.dungeon.definition;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Map;
import java.util.Objects;

public record SpawnerSpec(String id, EntityType entityType, String mythicType, String worldName,
                          double x, double y, double z, float yaw, float pitch,
                          int maxCount, int respawnDelayTicks) {
    public SpawnerSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(worldName, "worldName");
        if (entityType == null && (mythicType == null || mythicType.isBlank())) {
            throw new IllegalArgumentException("Spawner must specify either entityType or mythicType");
        }
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("id", id),
                Map.entry("entityType", entityType != null ? entityType.name() : ""),
                Map.entry("mythicType", mythicType != null ? mythicType : ""),
                Map.entry("world", worldName),
                Map.entry("x", x),
                Map.entry("y", y),
                Map.entry("z", z),
                Map.entry("yaw", yaw),
                Map.entry("pitch", pitch),
                Map.entry("maxCount", maxCount),
                Map.entry("respawnDelayTicks", respawnDelayTicks)
        );
    }

    public static SpawnerSpec fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.getOrDefault("id", "spawner_1");
        String typeStr = (String) map.getOrDefault("entityType", "ZOMBIE");
        EntityType type = null;
        try {
            if (typeStr != null && !typeStr.isBlank()) type = EntityType.valueOf(typeStr);
        } catch (IllegalArgumentException ignored) {}
        String mythic = (String) map.getOrDefault("mythicType", "");
        String world = (String) map.getOrDefault("world", "world");
        double x = ((Number) map.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) map.getOrDefault("y", 0)).doubleValue();
        double z = ((Number) map.getOrDefault("z", 0)).doubleValue();
        float yaw = ((Number) map.getOrDefault("yaw", 0)).floatValue();
        float pitch = ((Number) map.getOrDefault("pitch", 0)).floatValue();
        int maxCount = ((Number) map.getOrDefault("maxCount", 5)).intValue();
        int delay = ((Number) map.getOrDefault("respawnDelayTicks", 100)).intValue();
        return new SpawnerSpec(id, type, mythic, world, x, y, z, yaw, pitch, maxCount, delay);
    }
}
