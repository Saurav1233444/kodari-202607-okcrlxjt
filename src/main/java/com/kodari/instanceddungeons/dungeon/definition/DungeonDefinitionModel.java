package com.kodari.instanceddungeons.dungeon.definition;

import com.kodari.instanceddungeons.domain.DungeonDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DungeonDefinitionModel {
    private final UUID id;
    private String key;
    private String displayName;
    private String worldName;
    private Location entryLocation;
    private RegionSpec regionSpec;
    private int timeLimitSeconds;
    private final List<DungeonStage> stages;
    private final List<LootSpec> lootTables;

    public DungeonDefinitionModel(UUID id, String key, String displayName, String worldName,
                                 Location entryLocation, RegionSpec regionSpec,
                                 int timeLimitSeconds, List<DungeonStage> stages, List<LootSpec> lootTables) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.entryLocation = entryLocation;
        this.regionSpec = regionSpec;
        this.timeLimitSeconds = timeLimitSeconds <= 0 ? 1800 : timeLimitSeconds;
        this.stages = stages == null ? new ArrayList<>() : new ArrayList<>(stages);
        this.lootTables = lootTables == null ? new ArrayList<>() : new ArrayList<>(lootTables);
    }

    public UUID getId() { return id; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = Objects.requireNonNull(key); }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = Objects.requireNonNull(displayName); }
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = Objects.requireNonNull(worldName); }
    public Location getEntryLocation() { return entryLocation; }
    public void setEntryLocation(Location entryLocation) { this.entryLocation = entryLocation; }
    public RegionSpec getRegionSpec() { return regionSpec; }
    public void setRegionSpec(RegionSpec regionSpec) { this.regionSpec = regionSpec; }
    public int getTimeLimitSeconds() { return timeLimitSeconds; }
    public void setTimeLimitSeconds(int timeLimitSeconds) { this.timeLimitSeconds = timeLimitSeconds; }
    public List<DungeonStage> getStages() { return List.copyOf(stages); }
    public void setStages(List<DungeonStage> stages) {
        this.stages.clear();
        if (stages != null) this.stages.addAll(stages);
    }
    public List<LootSpec> getLootTables() { return List.copyOf(lootTables); }
    public void setLootTables(List<LootSpec> lootTables) {
        this.lootTables.clear();
        if (lootTables != null) this.lootTables.addAll(lootTables);
    }

    public Map<String, Object> toDataMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timeLimitSeconds", timeLimitSeconds);
        if (entryLocation != null) {
            map.put("entryLocation", Map.of(
                    "world", entryLocation.getWorld() != null ? entryLocation.getWorld().getName() : worldName,
                    "x", entryLocation.getX(),
                    "y", entryLocation.getY(),
                    "z", entryLocation.getZ(),
                    "yaw", entryLocation.getYaw(),
                    "pitch", entryLocation.getPitch()
            ));
        }
        if (regionSpec != null) {
            map.put("regionSpec", regionSpec.toMap());
        }
        map.put("stages", stages.stream().map(DungeonStage::toMap).toList());
        map.put("lootTables", lootTables.stream().map(LootSpec::toMap).toList());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static DungeonDefinitionModel fromDomain(DungeonDefinition domain) {
        if (domain == null) return null;
        Map<String, Object> data = domain.data();
        int timeLimit = ((Number) data.getOrDefault("timeLimitSeconds", 1800)).intValue();
        
        Location entry = null;
        Map<String, Object> entryMap = (Map<String, Object>) data.get("entryLocation");
        if (entryMap != null) {
            String w = (String) entryMap.getOrDefault("world", domain.templateWorld());
            double x = ((Number) entryMap.getOrDefault("x", 0)).doubleValue();
            double y = ((Number) entryMap.getOrDefault("y", 0)).doubleValue();
            double z = ((Number) entryMap.getOrDefault("z", 0)).doubleValue();
            float yaw = ((Number) entryMap.getOrDefault("yaw", 0)).floatValue();
            float pitch = ((Number) entryMap.getOrDefault("pitch", 0)).floatValue();
            entry = new Location(Bukkit.getWorld(w), x, y, z, yaw, pitch);
        }

        Map<String, Object> regMap = (Map<String, Object>) data.get("regionSpec");
        RegionSpec reg = RegionSpec.fromMap(regMap);

        List<Map<String, Object>> stageMaps = (List<Map<String, Object>>) data.get("stages");
        List<DungeonStage> stages = stageMaps == null ? List.of() :
                stageMaps.stream().map(DungeonStage::fromMap).filter(Objects::nonNull).toList();

        List<Map<String, Object>> lootMaps = (List<Map<String, Object>>) data.get("lootTables");
        List<LootSpec> loots = lootMaps == null ? List.of() :
                lootMaps.stream().map(LootSpec::fromMap).filter(Objects::nonNull).toList();

        return new DungeonDefinitionModel(domain.id(), domain.key(), domain.displayName(),
                domain.templateWorld(), entry, reg, timeLimit, stages, loots);
    }

    public DungeonDefinition toDomain() {
        return new DungeonDefinition(id, key, displayName, worldName, toDataMap(), System.currentTimeMillis());
    }
}
