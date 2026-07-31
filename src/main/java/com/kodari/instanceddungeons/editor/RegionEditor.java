package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.RegionSpec;
import org.bukkit.Location;

public class RegionEditor {
    private String worldName = "world";
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;

    public RegionEditor world(String worldName) { this.worldName = worldName; return this; }
    public RegionEditor bounds(Location corner1, Location corner2) {
        if (corner1 != null && corner2 != null && corner1.getWorld() != null) {
            this.worldName = corner1.getWorld().getName();
            this.minX = Math.min(corner1.getX(), corner2.getX());
            this.minY = Math.min(corner1.getY(), corner2.getY());
            this.minZ = Math.min(corner1.getZ(), corner2.getZ());
            this.maxX = Math.max(corner1.getX(), corner2.getX());
            this.maxY = Math.max(corner1.getY(), corner2.getY());
            this.maxZ = Math.max(corner1.getZ(), corner2.getZ());
        }
        return this;
    }

    public RegionSpec build() {
        return new RegionSpec(worldName, minX, minY, minZ, maxX, maxY, maxZ);
    }
}
