package com.kodari.instanceddungeons.dungeon.definition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DungeonValidator {

    private DungeonValidator() {}

    public static List<String> validate(DungeonDefinitionModel model) {
        Objects.requireNonNull(model, "model");
        List<String> errors = new ArrayList<>();

        if (model.getKey() == null || model.getKey().isBlank()) {
            errors.add("Dungeon key cannot be blank.");
        } else if (!model.getKey().matches("^[a-zA-Z0-9_-]+$")) {
            errors.add("Dungeon key must contain only alphanumeric characters, underscores, or hyphens.");
        }

        if (model.getDisplayName() == null || model.getDisplayName().isBlank()) {
            errors.add("Display name cannot be blank.");
        }

        if (model.getWorldName() == null || model.getWorldName().isBlank()) {
            errors.add("World name cannot be blank.");
        }

        if (model.getTimeLimitSeconds() <= 0) {
            errors.add("Time limit must be positive.");
        }

        List<DungeonStage> stages = model.getStages();
        if (stages.isEmpty()) {
            errors.add("Dungeon must contain at least one stage.");
        } else {
            for (int i = 0; i < stages.size(); i++) {
                DungeonStage stage = stages.get(i);
                if (stage == null) {
                    errors.add("Stage at index " + i + " is null.");
                    continue;
                }
                if (stage.name() == null || stage.name().isBlank()) {
                    errors.add("Stage " + (i + 1) + " has an empty name.");
                }
                if (stage.objectives().isEmpty()) {
                    errors.add("Stage " + (i + 1) + " (" + stage.name() + ") has no objectives.");
                }
                for (ObjectiveSpec obj : stage.objectives()) {
                    if (obj.requiredAmount() <= 0) {
                        errors.add("Objective " + obj.id() + " in stage " + stage.name() + " has requiredAmount <= 0.");
                    }
                }
            }
        }

        return List.copyOf(errors);
    }
}
