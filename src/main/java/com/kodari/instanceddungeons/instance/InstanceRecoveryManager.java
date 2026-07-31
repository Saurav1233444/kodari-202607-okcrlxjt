package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.database.repository.RepositoryRegistry;
import com.kodari.instanceddungeons.domain.InstanceRecord;
import com.kodari.instanceddungeons.dungeon.definition.DungeonDefinitionModel;
import com.kodari.instanceddungeons.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Runs once during plugin enable to detect and clean up orphaned instances
 * left over from a crash or unexpected stop.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Query all active (non-terminal) instance records from the database.</li>
 *   <li>For each, attempt to restore the player's saved entry location if they
 *       are already connected, or mark the snapshot for restoration on next join.</li>
 *   <li>Mark every orphaned record FAILED in the database and remove it from
 *       the runtime registry.</li>
 * </ol>
 *
 * <p>This runs asynchronously and dispatches the player teleport back onto the
 * main thread once DB work is completed.
 */
public final class InstanceRecoveryManager {
    private final JavaPlugin plugin;
    private final RepositoryRegistry repositories;
    private final SafeTeleportService teleportService;
    private final LoggingService logging;

    public InstanceRecoveryManager(JavaPlugin plugin, RepositoryRegistry repositories,
                                    SafeTeleportService teleportService, LoggingService logging) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repositories = Objects.requireNonNull(repositories, "repositories");
        this.teleportService = Objects.requireNonNull(teleportService, "teleportService");
        this.logging = Objects.requireNonNull(logging, "logging");
    }

    /**
     * Performs crash recovery.  Should be called once after the database is ready.
     * Runs async internally; never blocks the main thread.
     */
    public void recover() {
        repositories.instances().findActive().whenCompleteAsync((records, error) -> {
            if (error != null) {
                logging.error("Crash recovery failed — could not load active instance records.", error);
                return;
            }
            if (records == null || records.isEmpty()) {
                logging.info("Crash recovery: no orphaned instances found.");
                return;
            }
            logging.info("Crash recovery: processing orphaned instances.",
                    Map.of("count", records.size()));

            for (InstanceRecord record : records) {
                handleOrphanedRecord(record);
            }
        });
    }

    private void handleOrphanedRecord(InstanceRecord record) {

        // Fetch the definition so recovery can restore the player to the right place.
        // If the definition is missing, still mark the record failed and warn.
        repositories.dungeonDefinitions().findById(record.dungeonId())
                .whenCompleteAsync((definition, error) -> {
                    if (error != null) {
                        logging.error("Crash recovery: failed to load definition for orphaned instance.",
                                error);
                    }

                    // Build a minimal model purely for entry-location access if definition exists.
                    DungeonDefinitionModel model = (definition != null)
                            ? DungeonDefinitionModel.fromDomain(definition)
                            : null;

                    // Mark record as FAILED in DB
                    InstanceRecord failedRecord = new InstanceRecord(
                            record.id(),
                            record.dungeonId(),
                            record.worldName(),
                            DungeonInstanceState.FAILED.name(),
                            record.createdAt(),
                            System.currentTimeMillis()
                    );
                    repositories.instances().save(failedRecord);

                    // Attempt player restoration on the main thread
                    Bukkit.getScheduler().runTask(plugin, () -> restorePlayer(record, model));
                });
    }

    private void restorePlayer(InstanceRecord record, DungeonDefinitionModel model) {
        // Try to find a player UUID from runtime_state serialized_data.
        // For orphaned instances we rely on PlayerProgress records.
        // Since we don't know the player UUID from InstanceRecord alone, we look it up
        // via runtime state or skip if unavailable.
        repositories.runtimeState().find(record.id()).whenCompleteAsync((runtimeState, error) -> {
            if (error != null || runtimeState == null) {
                // No runtime state — cannot identify the player; nothing to restore
                return;
            }
            Object playerIdObj = runtimeState.data().get("playerId");
            if (!(playerIdObj instanceof String playerIdStr)) return;

            UUID playerId;
            try {
                playerId = UUID.fromString(playerIdStr);
            } catch (IllegalArgumentException ignored) {
                return;
            }

            // Delete stale runtime state
            repositories.runtimeState().delete(record.id());

            // Restore player if online
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    teleportService.restorePlayerLocation(player);
                    logging.info("Crash recovery: restored player.",
                            Map.of("player", player.getName(), "instance", record.id().toString()));
                } else {
                    // Player is offline — snapshot is already in service map if they were mid-dungeon.
                    // When they log in, InstanceRecoveryListener will handle them.
                    logging.info("Crash recovery: player offline, will restore on next login.",
                            Map.of("playerId", playerId.toString()));
                }
            });
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }
}
