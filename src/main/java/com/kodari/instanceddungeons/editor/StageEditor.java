package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.DungeonStage;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;
import com.kodari.instanceddungeons.dungeon.definition.SpawnerSpec;

import java.util.ArrayList;
import java.util.List;

public class StageEditor {
    private int index = 1;
    private String name = "Stage 1";
    private String description = "";
    private final List<ObjectiveSpec> objectives = new ArrayList<>();
    private final List<SpawnerSpec> spawners = new ArrayList<>();

    public StageEditor index(int index) { this.index = index; return this; }
    public StageEditor name(String name) { this.name = name; return this; }
    public StageEditor description(String description) { this.description = description; return this; }
    public StageEditor addObjective(ObjectiveSpec objective) { if (objective != null) this.objectives.add(objective); return this; }
    public StageEditor addSpawner(SpawnerSpec spawner) { if (spawner != null) this.spawners.add(spawner); return this; }

    public DungeonStage build() {
        return new DungeonStage(index, name, description, List.copyOf(objectives), List.copyOf(spawners));
    }
}
