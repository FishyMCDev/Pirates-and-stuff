package com.piratesmc.cutlasses;

import com.piratesmc.cutlasses.commands.CutlassCommand;
import com.piratesmc.cutlasses.config.ConfigManager;
import com.piratesmc.cutlasses.listeners.BlockListener;
import com.piratesmc.cutlasses.listeners.DamageListener;
import com.piratesmc.cutlasses.listeners.DodgeListener;
import com.piratesmc.cutlasses.listeners.FruitListener;
import com.piratesmc.cutlasses.listeners.GrapplingHookListener;
import com.piratesmc.cutlasses.listeners.MovementListener;
import com.piratesmc.cutlasses.listeners.ReplaceListener;
import com.piratesmc.cutlasses.mechanics.BlockHandler;
import com.piratesmc.cutlasses.mechanics.DodgeHandler;
import com.piratesmc.cutlasses.mechanics.StaminaManager;
import com.piratesmc.cutlasses.ui.StaminaBarDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;

public class CutlassesPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private CutlassManager cutlassManager;
    private BlockHandler blockHandler;
    private StaminaManager staminaManager;
    private DodgeHandler dodgeHandler;
    private StaminaBarDisplay staminaBar;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.configManager  = new ConfigManager(this);
        this.configManager.reload();

        this.cutlassManager = new CutlassManager(this);
        this.blockHandler   = new BlockHandler();
        this.staminaManager = new StaminaManager();
        this.dodgeHandler   = new DodgeHandler();
        this.staminaBar     = new StaminaBarDisplay(this);

        cutlassManager.rebuildRecipes();

        DodgeListener dodgeListener = new DodgeListener(this);
        getServer().getPluginManager().registerEvents(new BlockListener(this, dodgeListener), this);
        getServer().getPluginManager().registerEvents(dodgeListener, this);
        getServer().getPluginManager().registerEvents(new DamageListener(this), this);
        getServer().getPluginManager().registerEvents(new ReplaceListener(this), this);
        getServer().getPluginManager().registerEvents(new MovementListener(this), this);
        getServer().getPluginManager().registerEvents(new GrapplingHookListener(this), this);
        getServer().getPluginManager().registerEvents(new FruitListener(this), this);

        Objects.requireNonNull(getCommand("cutlass")).setExecutor(new CutlassCommand(this));
        Objects.requireNonNull(getCommand("cutlass")).setTabCompleter(new CutlassCommand(this));

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this, scheduled -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                UUID id = p.getUniqueId();

                if (blockHandler.isBlocking(id)) {
                    long now = System.currentTimeMillis();
                    long heldFor = now - blockHandler.getBlockStartedMs(id);
                    if (configManager.getBlockMaxHoldMs() > 0
                            && heldFor > configManager.getBlockMaxHoldMs()) {
                        blockHandler.cooldownFor(id, configManager.getBlockMaxHoldCooldownSeconds());
                        p.sendActionBar(Component.text("\u00a7cShield cooling down"));
                        p.setCooldown(p.getInventory().getItemInMainHand(),
                                configManager.getBlockMaxHoldCooldownSeconds() * 20);
                        continue;
                    }
                    if (!p.isHandRaised()) {
                        blockHandler.setBlocking(id, false);
                    }
                }
                
                if ((blockHandler.isDisabled(id) || blockHandler.isCoolingDown(id)) && p.isHandRaised()) {
                    p.clearActiveItem();
                    int ticks = Math.max(1, (int) Math.ceil(
                            Math.max(blockHandler.disabledRemainingMs(id), blockHandler.cooldownRemainingMs(id)) / 50.0));
                    p.setCooldown(p.getInventory().getItemInMainHand(), ticks);
                }

                org.bukkit.inventory.ItemStack mainHand = p.getInventory().getItemInMainHand();
                CutlassVariant variant = cutlassManager.getVariant(mainHand);
                if (variant == null) continue;
                String staminaKey = cutlassManager.getStaminaKey(mainHand);
                if (staminaKey == null) continue;

                staminaManager.tickCooldown(id, staminaKey);
                staminaBar.update(p, mainHand, variant);
                updateHotbarHud(p);
            }
        }, 5L, 5L);

        getLogger().info("FishyPirateItems enabled. Yo ho ho!");
    }

    @Override
    public void onDisable() {
        if (staminaBar != null) {
            for (UUID id : new java.util.ArrayList<>(staminaBar.all().keySet())) {
                staminaBar.remove(id);
            }
        }
        getLogger().info("FishyPirateItems disabled.");
    }

    public ConfigManager      cfg()       { return configManager; }
    public CutlassManager     cutlasses() { return cutlassManager; }
    public BlockHandler       blocks()    { return blockHandler; }
    public StaminaManager     stamina()   { return staminaManager; }
    public DodgeHandler       dodge()     { return dodgeHandler; }
    public StaminaBarDisplay  bar()       { return staminaBar; }

    private void updateHotbarHud(Player p) {
        if (blockHandler.isCoolingDown(p.getUniqueId())) {
            long shieldMs = blockHandler.cooldownRemainingMs(p.getUniqueId());
            if (shieldMs > 0L) {
                p.sendActionBar(Component.text(String.format(
                        "\u00a7cShield on cooldown %.1fs \u00a77| \u00a7fHold right-click + Sneak",
                        shieldMs / 1000.0)));
                return;
            }
        }

        long dodgeMs = dodgeHandler.cooldownRemainingMs(p.getUniqueId());
        org.bukkit.inventory.ItemStack mainHand = p.getInventory().getItemInMainHand();
        String staminaKey = cutlassManager.getStaminaKey(mainHand);
        long staminaMs = staminaKey == null ? 0L : staminaManager.cooldownRemainingMs(p.getUniqueId(), staminaKey);
        if (staminaMs > 0L) {
            p.sendActionBar(Component.text(String.format(
                    "\u00a7cNo stamina %.1fs \u00a77| \u00a7fHold right-click + Sneak",
                    staminaMs / 1000.0)));
            return;
        }
        if (staminaKey != null && !staminaManager.canBlock(p.getUniqueId(), staminaKey)) {
            p.sendActionBar(Component.text("\u00a7cNo stamina \u00a77| \u00a7fHold right-click + Sneak"));
            return;
        }
        if (dodgeMs <= 0L) {
            p.sendActionBar(Component.text("\u00a7aDash ready \u00a77| \u00a7fHold right-click + Sneak"));
            return;
        }

        p.sendActionBar(Component.text(String.format(
                "\u00a7eDash %.1fs \u00a77| \u00a7fHold right-click + Sneak",
                dodgeMs / 1000.0)));
    }
}
