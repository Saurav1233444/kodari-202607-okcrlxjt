package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.DungeonDefinitionModel;
import com.kodari.instanceddungeons.dungeon.definition.DungeonStage;
import com.kodari.instanceddungeons.dungeon.definition.LootSpec;
import com.kodari.instanceddungeons.dungeon.definition.RegionSpec;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DungeonEditor {
    private final DungeonDefinitionModel model;

    public DungeonEditor(DungeonDefinitionModel model) {
        this.model = Objects.requireNonNull(model, "model");
    }

    public DungeonEditor setKey(String key) {
        model.setKey(key);
        return this;
    }

    public DungeonEditor setDisplayName(String displayName) {
        model.setDisplayName(displayName);
        return this;
    }

    public DungeonEditor setWorldName(String worldName) {
        model.setWorldName(worldName);
        return this;
    }

    public DungeonEditor setEntryLocation(Location location) {
        model.setEntryLocation(location);
        return this;
    }

    public DungeonEditor setTimeLimitSeconds(int timeLimitSeconds) {
        model.setTimeLimitSeconds(timeLimitSeconds);
        return this;
    }

    public DungeonEditor setRegion(RegionSpec region) {
        model.setRegionSpec(region);
        return this;
    }

    public DungeonEditor addStage(DungeonStage stage) {
        List<DungeonStage> list = new ArrayList<>(model.getStages());
        list.add(stage);
        model.setStages(list);
        return this;
    }

    public DungeonEditor addLoot(LootSpec loot) {
        List<LootSpec> list = new ArrayList<>(model.getLootTables());
        list.add(loot);
        model.setLootTables(list);
        return this;
    }

    public DungeonDefinitionModel build() {
        return model;
    }
}
