package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;

/**
 * Tracks progress toward a single dungeon objective.
 *
 * <p>Implementations are held by a {@link DungeonInstance} and are updated via targeted
 * events rather than scanned on every tick.  They are never shared between instances.
 */
public interface ObjectiveTracker {

    /** Returns the spec this tracker was built from. */
    ObjectiveSpec spec();

    /** Returns how much progress has been made (0 – requiredAmount). */
    int progress();

    /** Returns {@code true} when the objective is fully satisfied. */
    boolean isComplete();

    /**
     * Records a unit of progress (e.g. one kill, one pickup).
     *
     * @param amount positive delta to apply
     */
    void advance(int amount);
}
