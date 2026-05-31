package com.piratesmc.cutlasses.config;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import com.piratesmc.cutlasses.PirateFruit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.Map;

public class ConfigManager {

    private final CutlassesPlugin plugin;
    private boolean replaceSwordDrops;
    private boolean replaceSwordLoot;
    private boolean replaceExistingOnJoin;
    private boolean removeVanillaRecipes;
    private double mobCutlassDropChance;
    private double mobDropEnchantChance;
    private boolean itemsUnbreakable;
    private int grapplingHookCooldownSeconds;
    private double grapplingHookMaxDistance;
    private double grapplingHookPullStrength;
    private double grapplingHookVerticalBoost;
    private int fruitDurationSeconds;
    private double fruitStaminaCooldownMultiplier;
    private final Map<PirateFruit, Integer> fruitDurationSecondsByFruit = new EnumMap<>(PirateFruit.class);
    private final Map<PirateFruit, Double> fruitHealValues = new EnumMap<>(PirateFruit.class);
    private final Map<PirateFruit, Double> fruitStaminaCooldownMultipliers = new EnumMap<>(PirateFruit.class);
    private final Map<PirateFruit, Integer> fruitExtraStaminaHits = new EnumMap<>(PirateFruit.class);
    private final Map<PirateFruit, Integer> fruitStaminaRestoreHits = new EnumMap<>(PirateFruit.class);
    private int staminaCooldownSeconds;
    private final Map<CutlassVariant, Integer> maxHits = new EnumMap<>(CutlassVariant.class);
    private double dodgeCooldownSeconds;
    private double dodgeVelocityMultiplier;
    private double dodgeVerticalBoost;
    private int dodgeIFramesTicks;
    private int axeDisableSeconds;
    private double attackerKnockback;
    private boolean attackerKnockbackEnabled;
    private int dashShieldDisableSeconds;
    private boolean playerHitSoundEnabled;
    private String playerHitSound;
    private int blockHoldExpireMs;
    private double blockMaxHoldSeconds;
    private int blockMaxHoldCooldownSeconds;
    private float blockConsumeSeconds;
    private float blockDelaySeconds;
    private float blockDisableCooldownScale;
    private float blockDamageReductionBase;
    private float blockDamageReductionFactor;
    private float blockHorizontalAngle;
    private boolean modelsEnabled;
    private String defaultItemModelNamespace;
    private final Map<CutlassVariant, Integer> modelIds = new EnumMap<>(CutlassVariant.class);
    private final Map<PirateFruit, Integer> fruitModelIds = new EnumMap<>(PirateFruit.class);
    private final Map<String, String> itemModels = new java.util.HashMap<>();
    private int grapplingHookModelId;
    private final Map<CutlassVariant, Double> damageValues = new EnumMap<>(CutlassVariant.class);

    public ConfigManager(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        replaceSwordDrops      = c.getBoolean("replacement.replace_vanilla_sword_drops", true);
        replaceSwordLoot       = c.getBoolean("replacement.replace_vanilla_sword_loot", true);
        replaceExistingOnJoin  = c.getBoolean("replacement.replace_existing_swords_on_join", false);
        removeVanillaRecipes   = c.getBoolean("replacement.remove_vanilla_sword_recipes", true);
        mobCutlassDropChance   = c.getDouble("mob_drops.cutlass_chance", 0.0003);
        mobDropEnchantChance   = c.getDouble("mob_drops.enchanted_chance", 0.04);

        itemsUnbreakable       = c.getBoolean("items.unbreakable", true);

        grapplingHookCooldownSeconds = c.getInt("grappling_hook.cooldown_seconds", 5);
        grapplingHookMaxDistance     = c.getDouble("grappling_hook.max_distance", 28.0);
        grapplingHookPullStrength    = c.getDouble("grappling_hook.pull_strength", 1.85);
        grapplingHookVerticalBoost   = c.getDouble("grappling_hook.vertical_boost", 0.35);

        fruitDurationSeconds = c.getInt("fruits.duration_seconds", 12);
        fruitStaminaCooldownMultiplier = c.getDouble("fruits.stamina_cooldown_multiplier", 0.5);

        staminaCooldownSeconds = c.getInt("stamina.cooldown_seconds", 20);

        dodgeCooldownSeconds   = c.getDouble("dodge.cooldown_seconds", 1.5);
        dodgeVelocityMultiplier= c.getDouble("dodge.velocity_multiplier", 1.8);
        dodgeVerticalBoost     = c.getDouble("dodge.vertical_boost", 0.3);
        dodgeIFramesTicks      = c.getInt("dodge.i_frames_ticks", 6);

        axeDisableSeconds      = c.getInt("blocking.axe_disable_seconds", 5);
        attackerKnockback      = c.getDouble("blocking.attacker_knockback", 0.6);
        attackerKnockbackEnabled = c.getBoolean("blocking.attacker_knockback_enabled", false);
        dashShieldDisableSeconds = c.getInt("blocking.dash_shield_disable_seconds", 3);
        playerHitSoundEnabled  = c.getBoolean("blocking.player_hit_sound_enabled",
                c.getBoolean("combat.player_hit_sound_enabled", true));
        playerHitSound         = c.getString("blocking.player_hit_sound",
                c.getString("combat.player_hit_sound", "entity.arrow.hit_player"));
        blockHoldExpireMs      = c.getInt("blocking.hold_expire_ms", 500);
        blockMaxHoldSeconds    = c.getDouble("blocking.max_hold_seconds", 20.0);
        blockMaxHoldCooldownSeconds = c.getInt("blocking.max_hold_cooldown_seconds", 5);
        blockConsumeSeconds    = (float)c.getDouble("blocking.consume_seconds", 720000.0);
        blockDelaySeconds      = (float)c.getDouble("blocking.block_delay_seconds", 0.0);
        blockDisableCooldownScale = (float)c.getDouble("blocking.disable_cooldown_scale", 0.0);
        blockDamageReductionBase  = (float)c.getDouble("blocking.damage_reduction_base", 0.0);
        blockDamageReductionFactor= (float)c.getDouble("blocking.damage_reduction_factor", 1.0);
        blockHorizontalAngle      = (float)c.getDouble("blocking.horizontal_blocking_angle", 180.0);

        modelsEnabled          = c.getBoolean("models.enabled", false);
        defaultItemModelNamespace = c.getString("models.default_item_model_namespace", "nexo");

        maxHits.clear();
        modelIds.clear();
        fruitModelIds.clear();
        itemModels.clear();
        fruitHealValues.clear();
        fruitDurationSecondsByFruit.clear();
        fruitStaminaCooldownMultipliers.clear();
        fruitExtraStaminaHits.clear();
        fruitStaminaRestoreHits.clear();
        damageValues.clear();
        for (CutlassVariant v : CutlassVariant.values()) {
            String k = v.getConfigKey();
            maxHits.put(v, c.getInt("stamina.variants." + k + ".max_hits", v.getDefaultMaxHits()));
            modelIds.put(v, c.getInt("models." + k, 0));
            itemModels.put(k, c.getString("models.item_model." + k, defaultItemModelNamespace + ":" + k));
            damageValues.put(v, c.getDouble("damage." + k, v.getDefaultDamage()));
        }
        grapplingHookModelId = c.getInt("models.grappling_hook", 1101);
        itemModels.put("grappling_hook", c.getString("models.item_model.grappling_hook", defaultItemModelNamespace + ":grappling_hook"));
        for (PirateFruit fruit : PirateFruit.values()) {
            String k = fruit.getConfigKey();
            fruitModelIds.put(fruit, c.getInt("models." + k, fruit.getDefaultModelId()));
            itemModels.put(k, c.getString("models.item_model." + k, defaultItemModelNamespace + ":" + k));
            fruitHealValues.put(fruit, c.getDouble("fruits." + k + ".heal", 4.0));
            fruitDurationSecondsByFruit.put(fruit, c.getInt("fruits." + k + ".duration_seconds", fruitDurationSeconds));
            fruitStaminaCooldownMultipliers.put(fruit, c.getDouble("fruits." + k + ".stamina_cooldown_multiplier", fruitStaminaCooldownMultiplier));
            fruitExtraStaminaHits.put(fruit, c.getInt("fruits." + k + ".extra_stamina_hits", 0));
            fruitStaminaRestoreHits.put(fruit, c.getInt("fruits." + k + ".stamina_restore_hits", 0));
        }
    }

    public boolean isReplaceSwordDrops()       { return replaceSwordDrops; }
    public boolean isReplaceSwordLoot()        { return replaceSwordLoot; }
    public boolean isReplaceExistingOnJoin()   { return replaceExistingOnJoin; }
    public boolean isRemoveVanillaRecipes()    { return removeVanillaRecipes; }
    public double getMobCutlassDropChance()    { return mobCutlassDropChance; }
    public double getMobDropEnchantChance()    { return mobDropEnchantChance; }
    public boolean isItemsUnbreakable()        { return itemsUnbreakable; }
    public int getGrapplingHookCooldownSeconds(){ return grapplingHookCooldownSeconds; }
    public double getGrapplingHookMaxDistance(){ return grapplingHookMaxDistance; }
    public double getGrapplingHookPullStrength(){ return grapplingHookPullStrength; }
    public double getGrapplingHookVerticalBoost(){ return grapplingHookVerticalBoost; }
    public int getFruitDurationSeconds()       { return fruitDurationSeconds; }
    public int getFruitDurationSeconds(PirateFruit fruit){ return fruitDurationSecondsByFruit.getOrDefault(fruit, fruitDurationSeconds); }
    public double getFruitStaminaCooldownMultiplier(){ return fruitStaminaCooldownMultiplier; }
    public double getFruitStaminaCooldownMultiplier(PirateFruit fruit){ return fruitStaminaCooldownMultipliers.getOrDefault(fruit, fruitStaminaCooldownMultiplier); }
    public double getFruitHeal(PirateFruit fruit){ return fruitHealValues.getOrDefault(fruit, 4.0); }
    public int getFruitExtraStaminaHits(PirateFruit fruit){ return fruitExtraStaminaHits.getOrDefault(fruit, 0); }
    public int getFruitStaminaRestoreHits(PirateFruit fruit){ return fruitStaminaRestoreHits.getOrDefault(fruit, 0); }
    public int  getStaminaCooldownSeconds()    { return staminaCooldownSeconds; }
    public int  getMaxHits(CutlassVariant v)   { return maxHits.getOrDefault(v, v.getDefaultMaxHits()); }
    public double getDodgeCooldownSeconds()    { return dodgeCooldownSeconds; }
    public double getDodgeVelocityMultiplier() { return dodgeVelocityMultiplier; }
    public double getDodgeVerticalBoost()      { return dodgeVerticalBoost; }
    public int  getDodgeIFramesTicks()         { return dodgeIFramesTicks; }
    public int  getAxeDisableSeconds()         { return axeDisableSeconds; }
    public double getAttackerKnockback()       { return attackerKnockback; }
    public boolean isAttackerKnockbackEnabled(){ return attackerKnockbackEnabled; }
    public int getDashShieldDisableSeconds()   { return dashShieldDisableSeconds; }
    public boolean isPlayerHitSoundEnabled()   { return playerHitSoundEnabled; }
    public String getPlayerHitSound()          { return playerHitSound; }
    public int  getBlockHoldExpireMs()         { return blockHoldExpireMs; }
    public double getBlockMaxHoldSeconds()     { return blockMaxHoldSeconds; }
    public long getBlockMaxHoldMs()            { return Math.round(blockMaxHoldSeconds * 1000.0); }
    public int getBlockMaxHoldCooldownSeconds(){ return blockMaxHoldCooldownSeconds; }
    public float getBlockConsumeSeconds()      { return blockConsumeSeconds; }
    public float getBlockDelaySeconds()        { return blockDelaySeconds; }
    public float getBlockDisableCooldownScale(){ return blockDisableCooldownScale; }
    public float getBlockDamageReductionBase() { return blockDamageReductionBase; }
    public float getBlockDamageReductionFactor(){ return blockDamageReductionFactor; }
    public float getBlockHorizontalAngle()     { return blockHorizontalAngle; }
    public boolean isModelsEnabled()           { return modelsEnabled; }
    public int  getModelId(CutlassVariant v)   { return modelIds.getOrDefault(v, 0); }
    public int getGrapplingHookModelId()       { return grapplingHookModelId; }
    public int getFruitModelId(PirateFruit fruit){ return fruitModelIds.getOrDefault(fruit, fruit.getDefaultModelId()); }
    public String getItemModel(String itemId)  { return itemModels.getOrDefault(itemId, defaultItemModelNamespace + ":" + itemId); }
    public double getDamage(CutlassVariant v)  { return damageValues.getOrDefault(v, v.getDefaultDamage()); }
}
