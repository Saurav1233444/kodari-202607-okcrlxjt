package com.kodari.instanceddungeons.instance;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SafeTeleportService {
    private final Map<UUID, PlayerLocationSnapshot> savedSnapshots = new ConcurrentHashMap<>();

    public CompletableFuture<Boolean> teleportIntoDungeon(Player player, Location dungeonTargetLocation) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(dungeonTargetLocation, "dungeonTargetLocation");

        // Save pre-teleport location & stats
        PlayerLocationSnapshot snapshot = PlayerLocationSnapshot.fromPlayer(player);
        savedSnapshots.put(player.getUniqueId(), snapshot);

        return player.teleportAsync(dungeonTargetLocation);
    }

    public void restorePlayerLocation(Player player) {
        if (player == null) return;
        PlayerLocationSnapshot snapshot = savedSnapshots.remove(player.getUniqueId());
        if (snapshot != null) {
            snapshot.restorePlayer(player);
        } else if (player.isOnline()) {
            player.teleportAsync(player.getWorld().getSpawnLocation());
        }
    }

    public void saveSnapshotDirectly(UUID playerId, PlayerLocationSnapshot snapshot) {
        if (playerId != null && snapshot != null) {
            savedSnapshots.put(playerId, snapshot);
        }
    }

    public Optional<PlayerLocationSnapshot> getSnapshot(UUID playerId) {
        return Optional.ofNullable(savedSnapshots.get(playerId));
    }

    public Optional<PlayerLocationSnapshot> removeSnapshot(UUID playerId) {
        return Optional.ofNullable(savedSnapshots.remove(playerId));
    }
}
