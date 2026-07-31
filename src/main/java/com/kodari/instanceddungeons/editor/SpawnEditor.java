package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.SpawnerSpec;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

public class SpawnEditor {
    private String id = "spawner_1";
    private EntityType entityType = EntityType.ZOMBIE;
    private String mythicType = "";
    private String worldName = "world";
    private double x, y, z;
    private float yaw, pitch;
    private int maxCount = 5;
    private int respawnDelayTicks = 100;

    public SpawnEditor id(String id) { this.id = id; return this; }
    public SpawnEditor entityType(EntityType entityType) { this.entityType = entityType; return this; }
    public SpawnEditor mythicType(String mythicType) { this.mythicType = mythicType; return this; }
    public SpawnEditor location(Location loc) {
        if (loc != null && loc.getWorld() != null) {
            this.worldName = loc.getWorld().getName();
            this.x = loc.getX();
            this.y = loc.getY();
            this.z = loc.getZ();
            this.yaw = loc.getYaw();
            this.pitch = loc.getPitch();
        }
        return this;
    }
    public SpawnEditor maxCount(int maxCount) { this.maxCount = maxCount; return this; }
    public SpawnEditor respawnDelayTicks(int respawnDelayTicks) { this.respawnDelayTicks = respawnDelayTicks; return this; }

    public SpawnerSpec build() {
        return new SpawnerSpec(id, entityType, mythicType, worldName, x, y, z, yaw, pitch, maxCount, respawnDelayTicks);
    }
}
