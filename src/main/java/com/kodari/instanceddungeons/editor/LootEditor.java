package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.LootSpec;
import org.bukkit.Material;

public class LootEditor {
    private String id = "loot_1";
    private Material material = Material.DIAMOND;
    private int minAmount = 1;
    private int maxAmount = 1;
    private double chance = 1.0D;
    private double moneyReward = 0.0D;
    private int xpReward = 0;
    private String commandReward = "";

    public LootEditor id(String id) { this.id = id; return this; }
    public LootEditor material(Material material) { this.material = material; return this; }
    public LootEditor amount(int minAmount, int maxAmount) {
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        return this;
    }
    public LootEditor chance(double chance) { this.chance = chance; return this; }
    public LootEditor moneyReward(double moneyReward) { this.moneyReward = moneyReward; return this; }
    public LootEditor xpReward(int xpReward) { this.xpReward = xpReward; return this; }
    public LootEditor commandReward(String commandReward) { this.commandReward = commandReward; return this; }

    public LootSpec build() {
        return new LootSpec(id, material, minAmount, maxAmount, chance, moneyReward, xpReward, commandReward);
    }
}
