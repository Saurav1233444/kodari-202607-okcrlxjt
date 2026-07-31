package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.ObjectiveSpec;
import com.kodari.instanceddungeons.dungeon.definition.ObjectiveType;
import com.kodari.instanceddungeons.dungeon.definition.RegionSpec;

public class ObjectiveEditor {
    private String id = "obj_1";
    private ObjectiveType type = ObjectiveType.KILL_MOB;
    private String description = "Defeat enemies";
    private String targetIdentifier = "ZOMBIE";
    private int requiredAmount = 10;
    private RegionSpec targetRegion = null;

    public ObjectiveEditor id(String id) { this.id = id; return this; }
    public ObjectiveEditor type(ObjectiveType type) { this.type = type; return this; }
    public ObjectiveEditor description(String description) { this.description = description; return this; }
    public ObjectiveEditor targetIdentifier(String targetIdentifier) { this.targetIdentifier = targetIdentifier; return this; }
    public ObjectiveEditor requiredAmount(int requiredAmount) { this.requiredAmount = requiredAmount; return this; }
    public ObjectiveEditor targetRegion(RegionSpec targetRegion) { this.targetRegion = targetRegion; return this; }

    public ObjectiveSpec build() {
        return new ObjectiveSpec(id, type, description, targetIdentifier, requiredAmount, targetRegion);
    }
}
