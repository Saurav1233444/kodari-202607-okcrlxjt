package com.kodari.instanceddungeons.dungeon.definition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Logger;

public final class YamlDungeonLoader {
    private final File dungeonsFolder;
    private final Logger logger;

    public YamlDungeonLoader(File dataFolder, Logger logger) {
        Objects.requireNonNull(dataFolder, "dataFolder");
        this.dungeonsFolder = new File(dataFolder, "dungeons");
        this.logger = Objects.requireNonNull(logger, "logger");
        if (!dungeonsFolder.exists()) {
            dungeonsFolder.mkdirs();
        }
    }

    public List<DungeonDefinitionModel> loadAll() {
        List<DungeonDefinitionModel> list = new ArrayList<>();
        File[] files = dungeonsFolder.listFiles((dir, name) -> name.endsWith(".yml") || name.endsWith(".yaml"));
        if (files == null) return list;

        for (File file : files) {
            try {
                DungeonDefinitionModel model = loadFile(file);
                if (model != null) {
                    list.add(model);
                }
            } catch (Exception e) {
                logger.warning("Failed to load dungeon configuration file " + file.getName() + ": " + e.getMessage());
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    public DungeonDefinitionModel loadFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String key = config.getString("key", file.getName().replace(".yml", "").replace(".yaml", ""));
        String name = config.getString("displayName", key);
        String world = config.getString("world", "world");
        String idStr = config.getString("id");
        UUID id = (idStr != null && !idStr.isBlank()) ? UUID.fromString(idStr) : UUID.nameUUIDFromBytes(key.getBytes());
        int timeLimit = config.getInt("timeLimitSeconds", 1800);

        List<Map<String, Object>> stageMaps = (List<Map<String, Object>>) (List<?>) config.getMapList("stages");
        List<DungeonStage> stages = stageMaps.stream().map(DungeonStage::fromMap).filter(Objects::nonNull).toList();

        List<Map<String, Object>> lootMaps = (List<Map<String, Object>>) (List<?>) config.getMapList("lootTables");
        List<LootSpec> loot = lootMaps.stream().map(LootSpec::fromMap).filter(Objects::nonNull).toList();

        return new DungeonDefinitionModel(id, key, name, world, null, null, timeLimit, stages, loot);
    }

    public void save(DungeonDefinitionModel model) throws IOException {
        File file = new File(dungeonsFolder, model.getKey() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("id", model.getId().toString());
        config.set("key", model.getKey());
        config.set("displayName", model.getDisplayName());
        config.set("world", model.getWorldName());
        config.set("timeLimitSeconds", model.getTimeLimitSeconds());
        config.set("stages", model.getStages().stream().map(DungeonStage::toMap).toList());
        config.set("lootTables", model.getLootTables().stream().map(LootSpec::toMap).toList());
        config.save(file);
    }
}
