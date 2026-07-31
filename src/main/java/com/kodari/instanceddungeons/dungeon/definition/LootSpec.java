package com.kodari.instanceddungeons.dungeon.definition;

import org.bukkit.Material;

import java.util.Map;
import java.util.Objects;

public record LootSpec(String id, Material material, int minAmount, int maxAmount, double chance, double moneyReward, int xpReward, String commandReward) {
    public LootSpec {
        Objects.requireNonNull(id, "id");
        if (minAmount < 1) minAmount = 1;
        if (maxAmount < minAmount) maxAmount = minAmount;
        if (chance < 0.0D) chance = 0.0D;
        if (chance > 1.0D) chance = 1.0D;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "material", material != null ? material.name() : "AIR",
                "minAmount", minAmount,
                "maxAmount", maxAmount,
                "chance", chance,
                "moneyReward", moneyReward,
                "xpReward", xpReward,
                "commandReward", commandReward != null ? commandReward : ""
        );
    }

    public static LootSpec fromMap(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.getOrDefault("id", "loot_1");
        String matStr = (String) map.getOrDefault("material", "DIAMOND");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.DIAMOND;
        int min = ((Number) map.getOrDefault("minAmount", 1)).intValue();
        int max = ((Number) map.getOrDefault("maxAmount", 1)).intValue();
        double chance = ((Number) map.getOrDefault("chance", 1.0D)).doubleValue();
        double money = ((Number) map.getOrDefault("moneyReward", 0.0D)).doubleValue();
        int xp = ((Number) map.getOrDefault("xpReward", 0)).intValue();
        String cmd = (String) map.getOrDefault("commandReward", "");
        return new LootSpec(id, mat, min, max, chance, money, xp, cmd);
    }
}
