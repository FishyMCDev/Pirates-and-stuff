package com.piratesmc.cutlasses.ui;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import com.piratesmc.cutlasses.mechanics.StaminaManager;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaminaBarDisplay {

    private final CutlassesPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();

    public StaminaBarDisplay(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    private BossBar getOrCreate(Player p) {
        BossBar bar = bars.get(p.getUniqueId());
        if (bar == null) {
            bar = Bukkit.createBossBar("Block Stamina", BarColor.GREEN, BarStyle.SEGMENTED_10);
            bar.addPlayer(p);
            bars.put(p.getUniqueId(), bar);
        }
        return bar;
    }

    public void update(Player p, ItemStack cutlass, CutlassVariant variant) {
        String staminaKey = plugin.cutlasses().getStaminaKey(cutlass);
        if (staminaKey == null) return;
        StaminaManager.State s = plugin.stamina().ensure(p.getUniqueId(), staminaKey, variant,
                plugin.cfg().getMaxHits(variant));
        BossBar bar = getOrCreate(p);

        if (plugin.blocks().isDisabled(p.getUniqueId())) {
            long ms = plugin.blocks().disabledRemainingMs(p.getUniqueId());
            bar.setTitle(String.format("Axed! Cutlass disabled %.1fs", ms / 1000.0));
            bar.setColor(BarColor.RED);
            bar.setProgress(0.0);
        } else if (plugin.blocks().isCoolingDown(p.getUniqueId())) {
            long ms = plugin.blocks().cooldownRemainingMs(p.getUniqueId());
            double total;
            if (plugin.blocks().isOnDashCooldown(p.getUniqueId())) {
                bar.setTitle(String.format("Shield down after dash %.1fs", ms / 1000.0));
                total = plugin.cfg().getDashShieldDisableSeconds();
            } else {
                bar.setTitle(String.format("Shield cooling down %.1fs", ms / 1000.0));
                total = plugin.cfg().getBlockMaxHoldCooldownSeconds();
            }
            double progress = total > 0 ? Math.max(0, Math.min(1, 1.0 - ((ms / 1000.0) / total))) : 0;
            bar.setColor(BarColor.RED);
            bar.setProgress(progress);
        } else if (s.onCooldown) {
            long remainMs = plugin.stamina().cooldownRemainingMs(p.getUniqueId(), staminaKey);
            double secs = Math.max(0, remainMs / 1000.0);
            bar.setTitle(String.format("Block stamina cooldown %.1fs", secs));
            bar.setColor(BarColor.WHITE);
            double total = s.baseCooldownMs > 0L ? s.baseCooldownMs / 1000.0 : plugin.cfg().getStaminaCooldownSeconds();
            double progress = total > 0 ? Math.max(0, Math.min(1, 1.0 - (secs / total))) : 0;
            bar.setProgress(progress);
        } else {
            double progress = s.maxHits == 0 ? 0 : (double) s.currentHits / (double) s.maxHits;
            bar.setProgress(Math.max(0, Math.min(1, progress)));
            bar.setTitle("Block Stamina (" + s.currentHits + "/" + s.maxHits + ")");
            if (progress >= 0.6)      bar.setColor(BarColor.GREEN);
            else if (progress >= 0.3) bar.setColor(BarColor.YELLOW);
            else                      bar.setColor(BarColor.RED);
        }

        bar.setVisible(true);
    }

    public void update(Player p, CutlassVariant variant) {
        update(p, p.getInventory().getItemInMainHand(), variant);
    }

    public void hide(Player p) {
        BossBar bar = bars.get(p.getUniqueId());
        if (bar != null) bar.setVisible(false);
    }

    public void remove(UUID id) {
        BossBar bar = bars.remove(id);
        if (bar != null) {
            bar.removeAll();
        }
    }

    public Map<UUID, BossBar> all() { return bars; }
}
