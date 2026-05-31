package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassesPlugin;
import com.piratesmc.cutlasses.PirateFruit;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class FruitListener implements Listener {

    private final CutlassesPlugin plugin;

    public FruitListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        PirateFruit fruit = plugin.cutlasses().getFruit(e.getItem());
        if (fruit == null) return;

        Player p = e.getPlayer();
        double healed = heal(p, plugin.cfg().getFruitHeal(fruit));
        plugin.stamina().boostRegen(p.getUniqueId(),
                plugin.cfg().getFruitDurationSeconds(fruit),
                plugin.cfg().getFruitStaminaCooldownMultiplier(fruit),
                plugin.cfg().getFruitExtraStaminaHits(fruit),
                plugin.cfg().getFruitStaminaRestoreHits(fruit));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.7f, 1.2f);
        p.sendActionBar(Component.text(String.format("§a%s §7+%.1f health, faster stamina",
                stripColor(fruit.getDisplayName()), healed)));
    }

    private double heal(Player p, double amount) {
        AttributeInstance maxHealth = p.getAttribute(Attribute.MAX_HEALTH);
        double max = maxHealth == null ? 20.0 : maxHealth.getValue();
        double before = p.getHealth();
        p.setHealth(Math.min(max, before + Math.max(0.0, amount)));
        return p.getHealth() - before;
    }

    private String stripColor(String text) {
        return text.replaceAll("§.", "");
    }
}
