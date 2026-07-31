package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.dungeon.definition.DungeonDefinitionModel;
import com.kodari.instanceddungeons.dungeon.definition.DungeonStage;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Runtime state for a single solo dungeon run.
 *
 * <p>All fields that are mutated after construction use thread-safe types
 * ({@link AtomicReference}, {@link ConcurrentHashMap.KeySetView}).  The
 * instance never holds a reference to an online {@link org.bukkit.entity.Player}
 * to avoid memory leaks when a player disconnects.
 *
 * <p>Timers are expressed in milliseconds from {@link System#currentTimeMillis()}
 * so they survive across ticks without any periodic scanning.
 */
public final class DungeonInstance {

    private final UUID instanceId;
    private final UUID playerId;
    private final DungeonDefinitionModel definition;

    private final long startedAtMillis;
    private final long timeLimitMillis;

    private final AtomicReference<DungeonInstanceState> state =
            new AtomicReference<>(DungeonInstanceState.CREATED);

    /** UUIDs of entities spawned for this instance — populated by the spawner, cleared on cleanup. */
    private final Set<UUID> spawnedEntityIds = ConcurrentHashMap.newKeySet();

    /** Objective trackers for the current stage, indexed by objective id. */
    private final List<ObjectiveTracker> currentObjectives = new ArrayList<>();

    /** Index of the stage currently active (0-based). */
    private volatile int currentStageIndex = 0;

    /** Snapshot of where the player was before entering the dungeon. */
    private volatile PlayerLocationSnapshot entrySnapshot = null;

    /** Millisecond timestamp set when the instance reaches a terminal state. */
    private volatile long completedAtMillis = 0L;

    public DungeonInstance(UUID instanceId, UUID playerId, DungeonDefinitionModel definition) {
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.startedAtMillis = System.currentTimeMillis();
        this.timeLimitMillis = (long) definition.getTimeLimitSeconds() * 1000L;
        buildObjectivesForStage(0);
    }

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    public UUID instanceId() { return instanceId; }
    public UUID playerId()   { return playerId; }
    public DungeonDefinitionModel definition() { return definition; }

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    public DungeonInstanceState state() { return state.get(); }

    /**
     * Attempts a state transition.
     *
     * @param expected  current state
     * @param next      target state
     * @return {@code true} when the transition was accepted
     */
    public boolean transition(DungeonInstanceState expected, DungeonInstanceState next) {
        return state.compareAndSet(expected, next);
    }

    public boolean isTerminal() {
        DungeonInstanceState s = state.get();
        return s == DungeonInstanceState.COMPLETED
                || s == DungeonInstanceState.FAILED
                || s == DungeonInstanceState.CANCELLED;
    }

    // -------------------------------------------------------------------------
    // Timer
    // -------------------------------------------------------------------------

    public long startedAtMillis() { return startedAtMillis; }
    public long timeLimitMillis() { return timeLimitMillis; }
    public long completedAtMillis() { return completedAtMillis; }

    /** Returns {@code true} when the wall-clock time limit has elapsed. */
    public boolean isTimedOut() {
        return System.currentTimeMillis() - startedAtMillis >= timeLimitMillis;
    }

    public void markTerminal(long atMillis) {
        this.completedAtMillis = atMillis;
    }

    // -------------------------------------------------------------------------
    // Stages & objectives
    // -------------------------------------------------------------------------

    public int currentStageIndex() { return currentStageIndex; }

    public List<ObjectiveTracker> currentObjectives() {
        return Collections.unmodifiableList(currentObjectives);
    }

    /**
     * Returns {@code true} when every objective in the current stage is complete.
     */
    public boolean currentStageComplete() {
        for (ObjectiveTracker tracker : currentObjectives) {
            if (!tracker.isComplete()) return false;
        }
        return !currentObjectives.isEmpty();
    }

    /**
     * Advances to the next stage.  Populates objective trackers for the new stage.
     *
     * @return {@code true} if there is a next stage, {@code false} if all stages are done
     */
    public boolean advanceStage() {
        int next = currentStageIndex + 1;
        if (next >= definition.getStages().size()) {
            return false;
        }
        currentStageIndex = next;
        buildObjectivesForStage(next);
        return true;
    }

    private void buildObjectivesForStage(int index) {
        currentObjectives.clear();
        List<DungeonStage> stages = definition.getStages();
        if (index >= stages.size()) return;
        for (ObjectiveSpec spec : stages.get(index).objectives()) {
            currentObjectives.add(ObjectiveTrackerFactory.from(spec));
        }
    }

    // -------------------------------------------------------------------------
    // Spawned entities
    // -------------------------------------------------------------------------

    public void trackEntity(UUID entityId)   { spawnedEntityIds.add(entityId); }
    public void untrackEntity(UUID entityId) { spawnedEntityIds.remove(entityId); }
    public Set<UUID> spawnedEntityIds()      { return Collections.unmodifiableSet(spawnedEntityIds); }
    public boolean tracksEntity(UUID entityId) { return spawnedEntityIds.contains(entityId); }

    // -------------------------------------------------------------------------
    // Entry snapshot
    // -------------------------------------------------------------------------

    public PlayerLocationSnapshot entrySnapshot() { return entrySnapshot; }
    public void setEntrySnapshot(PlayerLocationSnapshot snapshot) { this.entrySnapshot = snapshot; }
}
