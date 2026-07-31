package com.kodari.instanceddungeons.dungeon.definition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record DungeonStage(int index, String name, String description,
                           List<ObjectiveSpec> objectives, List<SpawnerSpec> spawners) {
    public DungeonStage {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        spawners = spawners == null ? List.of() : List.copyOf(spawners);
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "index", index,
                "name", name,
                "description", description,
                "objectives", objectives.stream().map(ObjectiveSpec::toMap).collect(Collectors.toList()),
                "spawners", spawners.stream().map(SpawnerSpec::toMap).collect(Collectors.toList())
        );
    }

    @SuppressWarnings("unchecked")
    public static DungeonStage fromMap(Map<String, Object> map) {
        if (map == null) return null;
        int index = ((Number) map.getOrDefault("index", 1)).intValue();
        String name = (String) map.getOrDefault("name", "Stage 1");
        String desc = (String) map.getOrDefault("description", "");
        List<Map<String, Object>> objsList = (List<Map<String, Object>>) map.get("objectives");
        List<ObjectiveSpec> objectives = objsList == null ? List.of() :
                objsList.stream().map(ObjectiveSpec::fromMap).filter(Objects::nonNull).collect(Collectors.toList());
        List<Map<String, Object>> spawnersList = (List<Map<String, Object>>) map.get("spawners");
        List<SpawnerSpec> spawners = spawnersList == null ? List.of() :
                spawnersList.stream().map(SpawnerSpec::fromMap).filter(Objects::nonNull).collect(Collectors.toList());
        return new DungeonStage(index, name, desc, objectives, spawners);
    }
}
