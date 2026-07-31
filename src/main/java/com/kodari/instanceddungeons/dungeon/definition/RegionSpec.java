package com.kodari.instanceddungeons.dungeon.definition;

import org.bukkit.Location;

import java.util.Map;
import java.util.Objects;

public record RegionSpec(String worldName, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public RegionSpec {
        Objects.requireNonNull(worldName, "worldName");
        if (minX > maxX) { double temp = minX; minX = maxX; maxX = temp; }
        if (minY > maxY) { double temp = minY; minY = maxY; maxY = temp; }
        if (minZ > maxZ) { double temp = minZ; minZ = maxZ; maxZ = temp; }
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) return false;
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "world", worldName,
                "minX", minX, "minY", minY, "minZ", minZ,
                "maxX", maxX, "maxY", maxY, "maxZ", maxZ
        );
    }

    public static RegionSpec fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String world = (String) map.getOrDefault("world", "world");
        double minX = ((Number) map.getOrDefault("minX", 0)).doubleValue();
        double minY = ((Number) map.getOrDefault("minY", 0)).doubleValue();
        double minZ = ((Number) map.getOrDefault("minZ", 0)).doubleValue();
        double maxX = ((Number) map.getOrDefault("maxX", 0)).doubleValue();
        double maxY = ((Number) map.getOrDefault("maxY", 0)).doubleValue();
        double maxZ = ((Number) map.getOrDefault("maxZ", 0)).doubleValue();
        return new RegionSpec(world, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
