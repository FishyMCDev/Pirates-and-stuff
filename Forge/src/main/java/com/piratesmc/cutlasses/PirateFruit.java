package com.piratesmc.cutlasses;

import org.bukkit.Material;

public enum PirateFruit {
    SCURVY_LIME("scurvy_lime", "§aScurvy Lime", Material.APPLE, 1102),
    SUNKEN_MANGO("sunken_mango", "§6Sunken Mango", Material.GOLDEN_CARROT, 1103),
    CAPTAIN_COCONUT("captain_coconut", "§fCaptain's Coconut", Material.CHORUS_FRUIT, 1104);

    private final String configKey;
    private final String displayName;
    private final Material baseItem;
    private final int defaultModelId;

    PirateFruit(String configKey, String displayName, Material baseItem, int defaultModelId) {
        this.configKey = configKey;
        this.displayName = displayName;
        this.baseItem = baseItem;
        this.defaultModelId = defaultModelId;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getBaseItem() {
        return baseItem;
    }

    public int getDefaultModelId() {
        return defaultModelId;
    }

    public static PirateFruit fromKey(String key) {
        if (key == null) return null;
        String normalized = key.toLowerCase();
        for (PirateFruit fruit : values()) {
            if (fruit.configKey.equals(normalized) || fruit.name().equalsIgnoreCase(key)) {
                return fruit;
            }
        }
        return null;
    }
}
