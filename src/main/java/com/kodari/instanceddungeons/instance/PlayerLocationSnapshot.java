package com.kodari.instanceddungeons.instance;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record PlayerLocationSnapshot(UUID playerId, String worldName, double x, double y, double z, float yaw, float pitch,
                                      double health, int foodLevel, int level, float exp) {
    public PlayerLocationSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(worldName, "worldName");
    }

    public static PlayerLocationSnapshot fromPlayer(Player player) {
        Objects.requireNonNull(player, "player");
        Location loc = player.getLocation();
        return new PlayerLocationSnapshot(
                player.getUniqueId(),
                loc.getWorld() != null ? loc.getWorld().getName() : "world",
                loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getLevel(),
                player.getExp()
        );
    }

    public Location toLocation() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return new Location(world, x, y, z, yaw, pitch);
    }

    public void restorePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        player.teleportAsync(toLocation());
        player.setHealth(Math.min(health, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        player.setFoodLevel(foodLevel);
        player.setLevel(level);
        player.setExp(exp);
    }

    public Map<String, Object> toMap() {
        return Map.ofEntries(
                Map.entry("playerId", playerId.toString()),
                Map.entry("world", worldName),
                Map.entry("x", x),
                Map.entry("y", y),
                Map.entry("z", z),
                Map.entry("yaw", yaw),
                Map.entry("pitch", pitch),
                Map.entry("health", health),
                Map.entry("foodLevel", foodLevel),
                Map.entry("level", level),
                Map.entry("exp", exp)
        );
    }

    public static PlayerLocationSnapshot fromMap(Map<String, Object> map) {
        if (map == null) return null;
        UUID pid = UUID.fromString((String) map.get("playerId"));
        String world = (String) map.getOrDefault("world", "world");
        double x = ((Number) map.getOrDefault("x", 0)).doubleValue();
        double y = ((Number) map.getOrDefault("y", 0)).doubleValue();
        double z = ((Number) map.getOrDefault("z", 0)).doubleValue();
        float yaw = ((Number) map.getOrDefault("yaw", 0)).floatValue();
        float pitch = ((Number) map.getOrDefault("pitch", 0)).floatValue();
        double health = ((Number) map.getOrDefault("health", 20.0D)).doubleValue();
        int food = ((Number) map.getOrDefault("foodLevel", 20)).intValue();
        int level = ((Number) map.getOrDefault("level", 0)).intValue();
        float exp = ((Number) map.getOrDefault("exp", 0.0F)).floatValue();
        return new PlayerLocationSnapshot(pid, world, x, y, z, yaw, pitch, health, food, level, exp);
    }
}
