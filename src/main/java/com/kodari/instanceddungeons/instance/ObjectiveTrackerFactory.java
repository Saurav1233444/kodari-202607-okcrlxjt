package com.kodari.instanceddungeons.instance;

import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveType;

/**
 * Builds an {@link ObjectiveTracker} from an {@link ObjectiveSpec}.
 *
 * <p>Centralises tracker construction so the runtime manager never needs to
 * switch on objective type.  New types (REACH_REGION, SURVIVE_TIME, …) are
 * added here alone.
 */
public final class ObjectiveTrackerFactory {

    private ObjectiveTrackerFactory() {}

    public static ObjectiveTracker from(ObjectiveSpec spec) {
        ObjectiveType type = spec.type();
        return switch (type) {
            case KILL_MOB, DEFEAT_BOSS -> new KillObjectiveTracker(spec);
            case COLLECT_ITEM           -> new CollectObjectiveTracker(spec);
            default -> throw new IllegalArgumentException("No tracker registered for objective type: " + type);
        };
    }
}
