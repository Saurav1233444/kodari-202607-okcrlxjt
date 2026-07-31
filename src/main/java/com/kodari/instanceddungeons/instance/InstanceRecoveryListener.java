package com.kodari.instanceddungeons.instance;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

/**
 * Handles player join/quit events for instance recovery.
 *
 * <p>On join: if a saved snapshot exists (from a crash mid-dungeon), the player
 * is immediately teleported back to their pre-dungeon location.
 *
 * <p>On quit: if the player is in an active instance, the instance is cancelled
 * after the configured reconnect window via the runtime manager.
 */
public final class InstanceRecoveryListener implements Listener {
    private final SafeTeleportService teleportService;
    private final InstanceRuntimeManager runtimeManager;

    public InstanceRecoveryListener(SafeTeleportService teleportService,
                                     InstanceRuntimeManager runtimeManager) {
        this.teleportService = Objects.requireNonNull(teleportService, "teleportService");
        this.runtimeManager = Objects.requireNonNull(runtimeManager, "runtimeManager");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // If there's a saved snapshot for a crash-disconnected player, restore them.
        teleportService.getSnapshot(event.getPlayer().getUniqueId()).ifPresent(snapshot -> {
            teleportService.restorePlayerLocation(event.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // When a player disconnects mid-dungeon we cancel their instance immediately.
        // A future reconnect window (Phase 4) may delay this.
        runtimeManager.getInstanceForPlayer(event.getPlayer().getUniqueId())
                .ifPresent(instance -> runtimeManager.cancelInstance(instance.instanceId()));
    }
}
