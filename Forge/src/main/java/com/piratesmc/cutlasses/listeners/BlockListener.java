package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class BlockListener implements Listener {

    private final CutlassesPlugin plugin;
    private final DodgeListener dodgeListener;

    public BlockListener(CutlassesPlugin plugin, DodgeListener dodgeListener) {
        this.plugin = plugin;
        this.dodgeListener = dodgeListener;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ItemStack normalized = plugin.cutlasses().normalizeHeldSword(mainHand);
        if (normalized != mainHand) {
            p.getInventory().setItemInMainHand(normalized);
            mainHand = normalized;
        }
        CutlassVariant variant = plugin.cutlasses().getVariant(mainHand);
        if (variant == null) return;
        String staminaKey = plugin.cutlasses().getStaminaKey(mainHand);
        if (staminaKey == null) return;

        e.setUseInteractedBlock(Event.Result.DENY);
        plugin.blocks().refreshRightClick(p.getUniqueId());

        if (plugin.dodge().isOnCooldown(p.getUniqueId())) {
            denyUse(e, p, mainHand, plugin.dodge().cooldownRemainingMs(p.getUniqueId()));
            p.sendActionBar(Component.text("\u00a7cShield down after dash"));
            return;
        }

        if (plugin.blocks().isBlocking(p.getUniqueId())) {
            e.setUseItemInHand(Event.Result.ALLOW);
            if (p.isSneaking() && dodgeListener.tryDodge(p)) {
                e.setUseItemInHand(Event.Result.DENY);
            }
            return;
        }

        if (plugin.blocks().isDisabled(p.getUniqueId())) {
            denyUse(e, p, mainHand, plugin.blocks().disabledRemainingMs(p.getUniqueId()));
            p.sendActionBar(Component.text("\u00a7cCutlass disabled by axe!"));
            return;
        }
        if (plugin.blocks().isCoolingDown(p.getUniqueId())) {
            denyUse(e, p, mainHand, plugin.blocks().cooldownRemainingMs(p.getUniqueId()));
            if (plugin.blocks().isOnDashCooldown(p.getUniqueId())) {
                p.sendActionBar(Component.text("\u00a7cShield down after dash"));
            } else {
                p.sendActionBar(Component.text("\u00a7cShield cooling down"));
            }
            return;
        }
        if (!plugin.stamina().canBlock(p.getUniqueId(), staminaKey)
                && plugin.stamina().get(p.getUniqueId(), staminaKey) != null) {
            denyUse(e, p, mainHand, plugin.stamina().cooldownRemainingMs(p.getUniqueId(), staminaKey));
            p.sendActionBar(Component.text("\u00a7cBlock stamina on cooldown!"));
            return;
        }

        e.setUseItemInHand(Event.Result.ALLOW);
        plugin.stamina().ensure(p.getUniqueId(), staminaKey, variant, plugin.cfg().getMaxHits(variant));
        plugin.blocks().setBlocking(p.getUniqueId(), true);
        p.playSound(p.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.4f, 1.6f);
        p.sendActionBar(Component.text("\u00a7bBlocking"));
        plugin.bar().update(p, mainHand, variant);

        if (p.isSneaking() && dodgeListener.tryDodge(p)) {
            e.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent e) {
        if (plugin.cutlasses().isCutlass(e.getItem())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onHeldChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack newItem = p.getInventory().getItem(e.getNewSlot());
        ItemStack normalized = plugin.cutlasses().normalizeHeldSword(newItem);
        if (normalized != newItem) {
            p.getInventory().setItem(e.getNewSlot(), normalized);
            newItem = normalized;
        }
        CutlassVariant variant = plugin.cutlasses().getVariant(newItem);
        if (variant == null) {
            stopBlocking(p);
            plugin.bar().hide(p);
        } else {
            plugin.bar().update(p, newItem, variant);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        ItemStack dropped = e.getItemDrop().getItemStack();
        if (plugin.cutlasses().isCutlass(dropped)) {
            stopBlocking(p);
            plugin.bar().hide(p);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        stopBlocking(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        plugin.blocks().clear(p.getUniqueId());
        plugin.dodge().clear(p.getUniqueId());
        plugin.stamina().clear(p.getUniqueId());
        plugin.bar().remove(p.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.blocks().clear(p.getUniqueId());
        plugin.dodge().clear(p.getUniqueId());
        plugin.stamina().clear(p.getUniqueId());
        plugin.bar().remove(p.getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ItemStack normalized = plugin.cutlasses().normalizeCutlass(mainHand);
        if (normalized != mainHand) {
            p.getInventory().setItemInMainHand(normalized);
            mainHand = normalized;
        }
        CutlassVariant variant = plugin.cutlasses().getVariant(mainHand);
        if (variant != null) {
            plugin.bar().update(p, mainHand, variant);
        }
    }

    private void stopBlocking(Player p) {
        if (plugin.blocks().isBlocking(p.getUniqueId())) {
            plugin.blocks().setBlocking(p.getUniqueId(), false);
        }
    }

    private void denyUse(PlayerInteractEvent e, Player p, ItemStack item, long remainingMs) {
        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        p.clearActiveItem();
        int ticks = Math.max(1, (int) Math.ceil(remainingMs / 50.0));
        p.setCooldown(item, ticks);
    }
}
