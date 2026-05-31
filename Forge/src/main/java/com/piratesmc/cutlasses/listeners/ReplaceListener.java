package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassManager;
import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReplaceListener implements Listener {

    private final CutlassesPlugin plugin;

    public ReplaceListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!plugin.cfg().isReplaceSwordDrops()) return;

        CutlassVariant variant = rollMobDropVariant();
        if (variant != null) {
            e.getDrops().add(plugin.cutlasses().createDroppedCutlass(variant));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLootGenerate(LootGenerateEvent e) {
        if (!plugin.cfg().isReplaceSwordLoot()) return;
        List<ItemStack> loot = e.getLoot();
        for (int i = 0; i < loot.size(); i++) {
            ItemStack item = loot.get(i);
            if (item == null) continue;
            if (CutlassManager.isSword(item.getType())) {
                loot.set(i, plugin.cutlasses().addCutlassMechanics(item));
            }
        }
    }

    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        ItemStack item = e.getItem().getItemStack();
        if (item == null) return;
        if (!CutlassManager.isSword(item.getType())) return;
        if (plugin.cutlasses().isCutlass(item)) return;

        ItemStack cutlass = plugin.cutlasses().addCutlassMechanics(item);
        if (cutlass == null) return;
        e.getItem().setItemStack(cutlass);
    }

    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreativeClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (player.getGameMode() != GameMode.CREATIVE) return;

        org.bukkit.Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> {
            if (!player.isOnline()) return;
            replaceInInventory(player.getInventory());

            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()
                    && CutlassManager.isSword(cursor.getType())
                    && !plugin.cutlasses().isCutlass(cursor)) {
                ItemStack cutlass = plugin.cutlasses().addCutlassMechanics(cursor);
                if (cutlass != null) {
                    player.setItemOnCursor(cutlass);
                }
            }
        }, 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.cfg().isReplaceExistingOnJoin()) return;
        replaceInInventory(e.getPlayer().getInventory());
    }

    private void replaceInInventory(Inventory inv) {
        ItemStack[] contents = inv.getContents();
        boolean changed = false;
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item == null) continue;
            Material type = item.getType();
            if (plugin.cutlasses().isCutlass(item)) continue;
            if (CutlassManager.isSword(type)) {
                ItemStack cutlass = plugin.cutlasses().addCutlassMechanics(item);
                if (cutlass != null) {
                    contents[i] = cutlass;
                    changed = true;
                }
            }
        }
        if (changed) inv.setContents(contents);
    }

    private CutlassVariant rollMobDropVariant() {
        if (ThreadLocalRandom.current().nextDouble() >= plugin.cfg().getMobCutlassDropChance()) {
            return null;
        }

        double[] weights = {40.0, 25.0, 16.0, 10.0, 5.0, 3.0, 1.0};
        CutlassVariant[] variants = CutlassVariant.values();
        double total = 0.0;
        for (double weight : weights) total += weight;

        double roll = ThreadLocalRandom.current().nextDouble(total);
        for (int i = 0; i < variants.length; i++) {
            roll -= weights[i];
            if (roll <= 0.0) return variants[i];
        }
        return variants[variants.length - 1];
    }
}
