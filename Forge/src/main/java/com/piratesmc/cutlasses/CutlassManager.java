package com.piratesmc.cutlasses;

import com.piratesmc.cutlasses.config.ConfigManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CutlassManager {

    public static final NamespacedKey KEY_IS_CUTLASS = key("is_cutlass");
    public static final NamespacedKey KEY_ITEM_TYPE   = key("item_type");
    public static final NamespacedKey KEY_VARIANT    = key("variant");
    public static final NamespacedKey KEY_MODEL_ID   = key("custom_model_data");
    public static final NamespacedKey KEY_MECHANICS_LORE_VERSION = key("mechanics_lore_version");
    public static final NamespacedKey KEY_STAMINA_ID = key("stamina_id");
    public static final String ITEM_TYPE_GRAPPLING_HOOK = "grappling_hook";
    public static final String ITEM_TYPE_FRUIT = "fruit";
    private static final int MECHANICS_LORE_VERSION = 1;

    private static final NamespacedKey ATTACK_DAMAGE_MODIFIER_KEY =
            new NamespacedKey("piratesmc", "cutlass_attack_damage");

    private final CutlassesPlugin plugin;
    private final ConfigManager config;

    public CutlassManager(CutlassesPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.cfg();
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey("piratesmc", name);
    }

    public ItemStack createCutlass(CutlassVariant variant) {
        ItemStack item = new ItemStack(variant.getBaseItem(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(variant.getDisplayName());

        meta.setLore(defaultCutlassLore(variant, true));

        meta.setUnbreakable(config.isItemsUnbreakable());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IS_CUTLASS, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_VARIANT, PersistentDataType.STRING, variant.name());
        pdc.set(KEY_MODEL_ID, PersistentDataType.INTEGER, config.getModelId(variant));

        try {
            AttributeModifier mod = new AttributeModifier(
                    ATTACK_DAMAGE_MODIFIER_KEY,
                    config.getDamage(variant) - 1.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            );
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, mod);
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to set attack damage attribute: " + t.getMessage());
        }

        item.setItemMeta(meta);
        applyModelComponents(item, config.getModelId(variant), variant.getConfigKey());
        applySwordTiltAnimation(item);
        return item;
    }

    public ItemStack addCutlassMechanics(ItemStack original) {
        if (original == null || original.getType() == Material.AIR) return original;
        CutlassVariant variant = CutlassVariant.fromSword(original.getType());
        if (variant == null) return original;
        if (isCutlass(original)) return original;

        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_IS_CUTLASS, PersistentDataType.BYTE, (byte) 1);
        pdc.set(KEY_VARIANT, PersistentDataType.STRING, variant.name());
        pdc.set(KEY_MODEL_ID, PersistentDataType.INTEGER, config.getModelId(variant));
        ensureStaminaId(pdc);
        ensureStaminaId(pdc);

        appendMechanicsLore(meta, variant);
        item.setItemMeta(meta);
        applySwordTiltAnimation(item);
        return item;
    }

    public ItemStack createDroppedCutlass(CutlassVariant variant) {
        ItemStack item = createCutlass(variant);
        maybeApplyRareDropEnchant(item);
        return item;
    }

    public ItemStack createGrapplingHook() {
        ItemStack item = new ItemStack(Material.CARROT_ON_A_STICK, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName("§6Grappling Hook");
        List<String> lore = new ArrayList<>();
        lore.add("§7A hooked line for boarding, climbing, and trouble.");
        lore.add("§8» §7Right-click a solid block §8» §7Grapple");
        lore.add("§8» §7Cooldown: §f" + config.getGrapplingHookCooldownSeconds() + "s");
        meta.setLore(lore);
        meta.setUnbreakable(config.isItemsUnbreakable());

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ITEM_TYPE, PersistentDataType.STRING, ITEM_TYPE_GRAPPLING_HOOK);
        pdc.set(KEY_MODEL_ID, PersistentDataType.INTEGER, config.getGrapplingHookModelId());

        item.setItemMeta(meta);
        applyModelComponents(item, config.getGrapplingHookModelId(), ITEM_TYPE_GRAPPLING_HOOK);
        return item;
    }

    public ItemStack createFruit(PirateFruit fruit) {
        ItemStack item = new ItemStack(fruit.getBaseItem(), 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(fruit.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§7Pirate fruit packed for a long voyage.");
        lore.add("§8» §7Heals: §f" + trim(config.getFruitHeal(fruit)) + "§7 health");
        lore.add("§8» §7Stamina recovery: §fx" + trim(1.0 / config.getFruitStaminaCooldownMultiplier(fruit))
                + " §7for §f" + config.getFruitDurationSeconds(fruit) + "s");
        if (config.getFruitExtraStaminaHits(fruit) > 0) {
            lore.add("§8» §7Extra stamina: §f+" + config.getFruitExtraStaminaHits(fruit));
        }
        if (config.getFruitStaminaRestoreHits(fruit) > 0) {
            lore.add("§8» §7Restores stamina: §f+" + config.getFruitStaminaRestoreHits(fruit));
        }
        meta.setLore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(KEY_ITEM_TYPE, PersistentDataType.STRING, ITEM_TYPE_FRUIT);
        pdc.set(KEY_VARIANT, PersistentDataType.STRING, fruit.name());
        pdc.set(KEY_MODEL_ID, PersistentDataType.INTEGER, config.getFruitModelId(fruit));

        item.setItemMeta(meta);
        applyModelComponents(item, config.getFruitModelId(fruit), fruit.getConfigKey());
        return item;
    }

    private void applyModelComponents(ItemStack item, int modelId, String itemId) {
        if (!config.isModelsEnabled()) return;

        NamespacedKey itemModel = NamespacedKey.fromString(config.getItemModel(itemId));
        if (itemModel != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setItemModel(itemModel);
                item.setItemMeta(meta);
            }
        }

        if (modelId > 0) {
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                    CustomModelData.customModelData().addFloat((float) modelId));
        }
    }

    private void applySwordTiltAnimation(ItemStack item) {
        try {
            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                    .consumeSeconds(config.getBlockConsumeSeconds())
                    .animation(ItemUseAnimation.BLOCK)
                    .hasConsumeParticles(false)
                    .build());

        } catch (Throwable t) {
            plugin.getLogger().warning(
                    "[Cutlasses] Sword-tilt animation unavailable (requires Paper 1.21.2+): "
                            + t.getMessage());
        }
    }

    public CutlassVariant getVariant(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return null;
        if (!stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte flag = pdc.get(KEY_IS_CUTLASS, PersistentDataType.BYTE);
        if (flag == null || flag != (byte) 1) return null;
        String key = pdc.get(KEY_VARIANT, PersistentDataType.STRING);
        return CutlassVariant.fromKey(key);
    }

    public boolean isCutlass(ItemStack stack) {
        return getVariant(stack) != null;
    }

    public String getStaminaKey(ItemStack stack) {
        CutlassVariant variant = getVariant(stack);
        if (variant == null || !stack.hasItemMeta()) return null;
        String key = stack.getItemMeta().getPersistentDataContainer()
                .get(KEY_STAMINA_ID, PersistentDataType.STRING);
        return key != null ? key : "legacy:" + variant.name();
    }

    public boolean isGrapplingHook(ItemStack stack) {
        return ITEM_TYPE_GRAPPLING_HOOK.equals(getItemType(stack));
    }

    public PirateFruit getFruit(ItemStack stack) {
        if (!ITEM_TYPE_FRUIT.equals(getItemType(stack))) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        String key = meta.getPersistentDataContainer().get(KEY_VARIANT, PersistentDataType.STRING);
        return PirateFruit.fromKey(key);
    }

    public ItemStack normalizeCutlass(ItemStack stack) {
        CutlassVariant variant = getVariant(stack);
        if (variant == null) return stack;

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            ensureStaminaId(meta.getPersistentDataContainer());
            appendMechanicsLore(meta, variant);
            stack.setItemMeta(meta);
            applySwordTiltAnimation(stack);
        }
        return stack;
    }

    public ItemStack normalizeHeldSword(ItemStack stack) {
        ItemStack normalized = normalizeCutlass(stack);
        if (normalized != stack) return normalized;
        if (stack == null || !isSword(stack.getType())) return stack;
        if (isCutlass(stack)) return stack;

        return addCutlassMechanics(stack);
    }

    public void rebuildRecipes() {
        if (config.isRemoveVanillaRecipes()) {
            for (CutlassVariant variant : CutlassVariant.values()) {
                NamespacedKey vanillaKey = NamespacedKey.minecraft(variant.getVanillaSword()
                        .name().toLowerCase());
                Bukkit.removeRecipe(vanillaKey);
            }
        }

        for (CutlassVariant variant : CutlassVariant.values()) {
            NamespacedKey recipeKey = new NamespacedKey(plugin, "cutlass_" + variant.getConfigKey());
            Bukkit.removeRecipe(recipeKey);

            Material ingot = ingredientFor(variant);
            if (ingot == null) continue;

            ShapedRecipe recipe = new ShapedRecipe(recipeKey, createCutlass(variant));
            recipe.shape("X", "X", "S");
            recipe.setIngredient('X', new RecipeChoice.MaterialChoice(ingot));
            recipe.setIngredient('S', new RecipeChoice.MaterialChoice(Material.STICK));
            Bukkit.addRecipe(recipe);
        }

        NamespacedKey grappleKey = new NamespacedKey(plugin, "grappling_hook");
        Bukkit.removeRecipe(grappleKey);
        ShapedRecipe grapple = new ShapedRecipe(grappleKey, createGrapplingHook());
        grapple.shape(" SI", " CS", "C  ");
        grapple.setIngredient('S', new RecipeChoice.MaterialChoice(Material.STRING));
        grapple.setIngredient('I', new RecipeChoice.MaterialChoice(Material.IRON_INGOT));
        grapple.setIngredient('C', new RecipeChoice.MaterialChoice(Material.CARROT_ON_A_STICK));
        Bukkit.addRecipe(grapple);
    }

    private Material ingredientFor(CutlassVariant variant) {
        return switch (variant) {
            case WOOD      -> Material.OAK_PLANKS;
            case STONE     -> Material.COBBLESTONE;
            case COPPER    -> Material.COPPER_INGOT;
            case IRON      -> Material.IRON_INGOT;
            case GOLD      -> Material.GOLD_INGOT;
            case DIAMOND   -> Material.DIAMOND;
            case NETHERITE -> Material.NETHERITE_INGOT;
        };
    }

    public ItemStack cutlassForSword(Material material) {
        CutlassVariant variant = CutlassVariant.fromSword(material);
        return variant == null ? null : createCutlass(variant);
    }

    private List<String> defaultCutlassLore(CutlassVariant variant, boolean includeDamage) {
        List<String> lore = new ArrayList<>();
        lore.add("\u00a77A pirate's blade. Forged for combat at sea.");
        lore.addAll(mechanicsLore(variant));
        if (includeDamage) {
            lore.add("\u00a78\u00bb \u00a77Damage: \u00a7f" + config.getDamage(variant));
        }
        return lore;
    }

    private void appendMechanicsLore(ItemMeta meta, CutlassVariant variant) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer loreVersion = pdc.get(KEY_MECHANICS_LORE_VERSION, PersistentDataType.INTEGER);
        if (loreVersion != null && loreVersion >= MECHANICS_LORE_VERSION) return;

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        if (hasCutlassMechanicsLore(lore)) {
            pdc.set(KEY_MECHANICS_LORE_VERSION, PersistentDataType.INTEGER, MECHANICS_LORE_VERSION);
            return;
        }
        if (!lore.isEmpty()) {
            lore.add("");
        }
        lore.addAll(mechanicsLore(variant));
        meta.setLore(lore);
        pdc.set(KEY_MECHANICS_LORE_VERSION, PersistentDataType.INTEGER, MECHANICS_LORE_VERSION);
    }

    private List<String> mechanicsLore(CutlassVariant variant) {
        List<String> lore = new ArrayList<>();
        lore.add("\u00a78\u00bb \u00a77Hold right-click \u00a78\u00bb \u00a77Block");
        lore.add("\u00a78\u00bb \u00a77Hold right-click + sneak \u00a78\u00bb \u00a77Dodge");
        lore.add("\u00a78\u00bb \u00a77Block stamina: \u00a7f" + config.getMaxHits(variant) + " \u00a77hits");
        return lore;
    }

    private boolean hasCutlassMechanicsLore(List<String> lore) {
        for (String line : lore) {
            if (line != null && (line.contains("Block stamina:") || line.contains("Hold right-click"))) {
                return true;
            }
        }
        return false;
    }

    private void ensureStaminaId(PersistentDataContainer pdc) {
        if (!pdc.has(KEY_STAMINA_ID, PersistentDataType.STRING)) {
            pdc.set(KEY_STAMINA_ID, PersistentDataType.STRING, java.util.UUID.randomUUID().toString());
        }
    }

    private void maybeApplyRareDropEnchant(ItemStack item) {
        if (Math.random() >= config.getMobDropEnchantChance()) return;

        double roll = Math.random();
        if (roll < 0.55) {
            item.addUnsafeEnchantment(Enchantment.SHARPNESS, 1 + (int) (Math.random() * 5));
        } else if (roll < 0.85) {
            item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1 + (int) (Math.random() * 2));
        } else {
            item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        }
    }

    public static boolean isSword(Material m) {
        if (m == null) return false;
        return switch (m) {
            case WOODEN_SWORD, STONE_SWORD, COPPER_SWORD, IRON_SWORD,
                 GOLDEN_SWORD, DIAMOND_SWORD, NETHERITE_SWORD -> true;
            default -> false;
        };
    }

    public static List<Material> allPlanks() {
        return Arrays.asList(
                Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
                Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
                Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.BAMBOO_PLANKS,
                Material.CRIMSON_PLANKS, Material.WARPED_PLANKS, Material.PALE_OAK_PLANKS
        );
    }

    private String getItemType(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer().get(KEY_ITEM_TYPE, PersistentDataType.STRING);
    }

    private static String trim(double value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format("%.1f", value);
    }
}
