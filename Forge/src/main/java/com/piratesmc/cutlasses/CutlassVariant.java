package com.piratesmc.cutlasses;

import org.bukkit.Material;

public enum CutlassVariant {

    WOOD     ("Wooden",    Material.WOODEN_SWORD,    "wood",      4.0,  3,  "§e",  Material.WOODEN_SWORD),
    STONE    ("Stone",     Material.STONE_SWORD,     "stone",     5.0,  5,  "§7",  Material.STONE_SWORD),
    COPPER   ("Copper",    Material.COPPER_SWORD,    "copper",    5.0,  6,  "§c",  Material.COPPER_SWORD),
    IRON     ("Iron",      Material.IRON_SWORD,      "iron",      6.0,  8,  "§f",  Material.IRON_SWORD),
    GOLD     ("Golden",    Material.GOLDEN_SWORD,    "gold",      4.0,  4,  "§6",  Material.GOLDEN_SWORD),
    DIAMOND  ("Diamond",   Material.DIAMOND_SWORD,   "diamond",   7.0,  12, "§b",  Material.DIAMOND_SWORD),
    NETHERITE("Netherite", Material.NETHERITE_SWORD, "netherite", 8.0,  15, "§8",  Material.NETHERITE_SWORD);

    private final String displayPrefix;
    private final Material baseItem;
    private final String configKey;
    private final double defaultDamage;
    private final int defaultMaxHits;
    private final String colorCode;
    private final Material vanillaSword;

    CutlassVariant(String displayPrefix, Material baseItem, String configKey,
                   double defaultDamage, int defaultMaxHits, String colorCode,
                   Material vanillaSword) {
        this.displayPrefix = displayPrefix;
        this.baseItem = baseItem;
        this.configKey = configKey;
        this.defaultDamage = defaultDamage;
        this.defaultMaxHits = defaultMaxHits;
        this.colorCode = colorCode;
        this.vanillaSword = vanillaSword;
    }

    public String getDisplayPrefix() { return displayPrefix; }
    public Material getBaseItem() { return baseItem; }
    public String getConfigKey() { return configKey; }
    public double getDefaultDamage() { return defaultDamage; }
    public int getDefaultMaxHits() { return defaultMaxHits; }
    public String getColorCode() { return colorCode; }
    public Material getVanillaSword() { return vanillaSword; }

    public String getDisplayName() {
        return colorCode + displayPrefix + " Cutlass";
    }

    public static CutlassVariant fromSword(Material material) {
        for (CutlassVariant v : values()) {
            if (v.vanillaSword == material) return v;
        }
        return null;
    }

    public static CutlassVariant fromKey(String key) {
        if (key == null) return null;
        try {
            return CutlassVariant.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
