package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.database.repository.RepositoryRegistry;
import com.kodari.instanceddungeons.domain.InstanceRecord;
import com.kodari.instanceddungeons.dungeon.definition.DungeonDefinitionModel;
import com.kodari.instanceddungeons.logging.LoggingService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of every active {@link DungeonInstance}.
 *
 * <p>Registry uses a {@link ConcurrentHashMap} so reads from event handlers on
 * the main thread never block.  Structural mutations (start / finish) are
 * always performed on the main thread after async work completes.
 *
 * <p>No per-tick scanning — timers are driven by a single Bukkit scheduler task
 * fired once per second that only iterates running instances.
 */
public final class InstanceRuntimeManager {
    private final Map<UUID, DungeonInstance> instances = new ConcurrentHashMap<>();
    private final SafeTeleportService teleportService;
    private final RepositoryRegistry repositories;
    private final LoggingService logging;
    private final JavaPlugin plugin;
    private int tickTaskId = -1;

    public InstanceRuntimeManager(JavaPlugin plugin, SafeTeleportService teleportService,
                                   RepositoryRegistry repositories, LoggingService logging) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.teleportService = Objects.requireNonNull(teleportService, "teleportService");
        this.repositories = Objects.requireNonNull(repositories, "repositories");
        this.logging = Objects.requireNonNull(logging, "logging");
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    public void start() {
        // Check timers at 20-tick intervals (once per second) – never scans entities or chunks.
        tickTaskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickTimers, 20L, 20L).getTaskId();
        logging.info("InstanceRuntimeManager started.");
    }

    public void stop() {
        if (tickTaskId != -1) {
            Bukkit.getScheduler().cancelTask(tickTaskId);
            tickTaskId = -1;
        }
        // Do not despawn entities on shutdown — let the server handle it.
        instances.clear();
        logging.info("InstanceRuntimeManager stopped.");
    }

    // -------------------------------------------------------------------------
    // Instance management
    // -------------------------------------------------------------------------

    /**
     * Creates and registers a new instance.  Must be called on the main thread.
     *
     * @param player     the solo participant
     * @param definition the dungeon definition
     * @return the newly started instance
     */
    public CompletableFuture<DungeonInstance> startInstance(Player player, DungeonDefinitionModel definition) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(definition, "definition");

        UUID instanceId = UUID.randomUUID();
        DungeonInstance instance = new DungeonInstance(instanceId, player.getUniqueId(), definition);

        // Capture snapshot before teleport
        PlayerLocationSnapshot snapshot = PlayerLocationSnapshot.fromPlayer(player);
        instance.setEntrySnapshot(snapshot);
        teleportService.saveSnapshotDirectly(player.getUniqueId(), snapshot);

        // Persist the record asynchronously then teleport on main thread
        InstanceRecord record = new InstanceRecord(
                instanceId,
                definition.getId(),
                "instance-" + instanceId,   // logical identifier; no world folder created
                DungeonInstanceState.CREATED.name(),
                instance.startedAtMillis(),
                null
        );

        return repositories.instances().save(record).thenComposeAsync(ignored -> {
            instance.transition(DungeonInstanceState.CREATED, DungeonInstanceState.RUNNING);
            instances.put(instanceId, instance);

            if (definition.getEntryLocation() != null) {
                return teleportService.teleportIntoDungeon(player, definition.getEntryLocation())
                        .thenApply(ok -> instance);
            }
            return CompletableFuture.completedFuture(instance);
        }, runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    /**
     * Concludes an instance as completed.  Restores the player and schedules cleanup.
     */
    public void completeInstance(UUID instanceId) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null) return;
        if (!instance.transition(DungeonInstanceState.RUNNING, DungeonInstanceState.COMPLETED)) return;

        long now = System.currentTimeMillis();
        instance.markTerminal(now);
        finishInstance(instance, now);
    }

    /**
     * Concludes an instance as failed.  Restores the player and schedules cleanup.
     */
    public void failInstance(UUID instanceId) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null) return;
        if (!instance.transition(DungeonInstanceState.RUNNING, DungeonInstanceState.FAILED)) return;

        long now = System.currentTimeMillis();
        instance.markTerminal(now);
        finishInstance(instance, now);
    }

    /**
     * Cancels an instance (e.g. player disconnected and recovery window expired).
     */
    public void cancelInstance(UUID instanceId) {
        DungeonInstance instance = instances.get(instanceId);
        if (instance == null) return;
        DungeonInstanceState current = instance.state();
        if (current == DungeonInstanceState.CREATED || current == DungeonInstanceState.RUNNING) {
            instance.transition(current, DungeonInstanceState.CANCELLED);
        }
        long now = System.currentTimeMillis();
        instance.markTerminal(now);
        finishInstance(instance, now);
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    public Optional<DungeonInstance> getInstance(UUID instanceId) {
        return Optional.ofNullable(instances.get(instanceId));
    }

    /** Returns the active instance for a player, or empty if not in one. */
    public Optional<DungeonInstance> getInstanceForPlayer(UUID playerId) {
        for (DungeonInstance inst : instances.values()) {
            if (inst.playerId().equals(playerId) && !inst.isTerminal()) {
                return Optional.of(inst);
            }
        }
        return Optional.empty();
    }

    public int activeCount() { return instances.size(); }

    public Collection<DungeonInstance> allInstances() {
        return Collections.unmodifiableCollection(instances.values());
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void finishInstance(DungeonInstance instance, long terminatedAt) {
        UUID instanceId = instance.instanceId();

        // Despawn tracked entities on the main thread
        despawnEntities(instance);

        // Restore player if online
        Player player = Bukkit.getPlayer(instance.playerId());
        if (player != null && player.isOnline()) {
            teleportService.restorePlayerLocation(player);
        } else {
            // Player offline — keep snapshot until they reconnect or it expires
            teleportService.removeSnapshot(instance.playerId());
        }

        // Persist the terminal state asynchronously then remove from registry
        InstanceRecord terminal = new InstanceRecord(
                instanceId,
                instance.definition().getId(),
                "instance-" + instanceId,
                instance.state().name(),
                instance.startedAtMillis(),
                terminatedAt
        );
        repositories.instances().save(terminal)
                .thenRunAsync(() -> instances.remove(instanceId),
                        runnable -> Bukkit.getScheduler().runTask(plugin, runnable));
    }

    private void despawnEntities(DungeonInstance instance) {
        for (UUID entityId : instance.spawnedEntityIds()) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    /** Called once per second by the scheduler — only iterates running instances. */
    private void tickTimers() {
        for (DungeonInstance instance : instances.values()) {
            if (instance.state() != DungeonInstanceState.RUNNING) continue;
            if (instance.isTimedOut()) {
                logging.info("Instance timed out.", Map.of("instanceId", instance.instanceId().toString()));
                failInstance(instance.instanceId());
            }
        }
    }
}
