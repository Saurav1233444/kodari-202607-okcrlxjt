package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveType;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks COLLECT_ITEM objectives.
 *
 * <p>Designed to be updated from inventory-change events without scanning
 * inventory contents on every tick.
 */
public final class CollectObjectiveTracker implements ObjectiveTracker {
    private final ObjectiveSpec spec;
    private final AtomicInteger count = new AtomicInteger(0);

    public CollectObjectiveTracker(ObjectiveSpec spec) {
        Objects.requireNonNull(spec, "spec");
        if (spec.type() != ObjectiveType.COLLECT_ITEM) {
            throw new IllegalArgumentException("CollectObjectiveTracker requires COLLECT_ITEM, got " + spec.type());
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
