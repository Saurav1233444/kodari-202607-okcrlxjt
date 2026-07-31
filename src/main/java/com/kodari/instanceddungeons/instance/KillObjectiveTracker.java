package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks KILL_MOB and DEFEAT_BOSS objectives.
 *
 * <p>Thread-safe via {@link AtomicInteger} — multiple entity-death events
 * can arrive on the main thread but the counter is still correct.
 */
public final class KillObjectiveTracker implements ObjectiveTracker {
    private final ObjectiveSpec spec;
    private final AtomicInteger count = new AtomicInteger(0);

    public KillObjectiveTracker(ObjectiveSpec spec) {
        Objects.requireNonNull(spec, "spec");
        if (spec.type() != ObjectiveType.KILL_MOB && spec.type() != ObjectiveType.DEFEAT_BOSS) {
            throw new IllegalArgumentException("KillObjectiveTracker requires KILL_MOB or DEFEAT_BOSS, got " + spec.type());
        }
        this.spec = spec;
    }

    @Override
    public ObjectiveSpec spec() { return spec; }

    @Override
    public int progress() { return count.get(); }

    @Override
    public boolean isComplete() { return count.get() >= spec.requiredAmount(); }

    @Override
    public void advance(int amount) {
        if (amount > 0) {
            count.updateAndGet(current -> Math.min(current + amount, spec.requiredAmount()));
        }
    }
}
