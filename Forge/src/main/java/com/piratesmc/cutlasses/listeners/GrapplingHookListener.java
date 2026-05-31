package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassesPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GrapplingHookListener implements Listener {

    private static final int MAX_FISHING_WAIT_TICKS = 600;

    private final CutlassesPlugin plugin;
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();

    public GrapplingHookListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action action = e.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player p = e.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!plugin.cutlasses().isGrapplingHook(item)) return;

        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);

        long remainingMs = cooldownRemainingMs(p.getUniqueId());
        if (remainingMs > 0L) {
            p.setCooldown(item, Math.max(1, (int) Math.ceil(remainingMs / 50.0)));
            p.sendActionBar(Component.text(String.format("§7Grappling hook %.1fs", remainingMs / 1000.0)));
            return;
        }

        RayTraceResult result = p.rayTraceBlocks(plugin.cfg().getGrapplingHookMaxDistance(), FluidCollisionMode.NEVER);
        if (result == null || result.getHitBlock() == null || result.getHitPosition() == null) {
            p.sendActionBar(Component.text("§cNo solid anchor in range"));
            return;
        }

        Block block = result.getHitBlock();
        if (block.isPassable() || block.getType() == Material.AIR) {
            p.sendActionBar(Component.text("§cNo solid anchor in range"));
            return;
        }

        Vector anchor = result.getHitPosition().clone();
        Vector pull = anchor.clone().subtract(p.getEyeLocation().toVector());
        if (pull.lengthSquared() < 1.0) {
            p.sendActionBar(Component.text("§cAnchor too close"));
            return;
        }

        FishHook hook = p.launchProjectile(FishHook.class, pull.normalize().multiply(2.6), fired -> {
            fired.setShooter(p);
            fired.setApplyLure(false);
            fired.setMinWaitTime(MAX_FISHING_WAIT_TICKS);
            fired.setMaxWaitTime(MAX_FISHING_WAIT_TICKS);
        });
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 0.7f, 1.35f);
        monitorHook(p, hook, anchor);

        int cooldownTicks = Math.max(1, plugin.cfg().getGrapplingHookCooldownSeconds() * 20);
        cooldownUntil.put(p.getUniqueId(), System.currentTimeMillis() + plugin.cfg().getGrapplingHookCooldownSeconds() * 1000L);
        p.setCooldown(item, cooldownTicks);
    }

    private void monitorHook(Player p, FishHook hook, Vector anchor) {
        final int[] ticks = {0};
        p.getScheduler().runAtFixedRate(plugin, task -> {
            ticks[0] += 1;
            if (!p.isOnline() || hook.isDead() || !hook.isValid() || ticks[0] > 60) {
                hook.remove();
                task.cancel();
                return;
            }

            double distanceToAnchor = hook.getLocation().toVector().distanceSquared(anchor);
            if (distanceToAnchor > 1.44 && hook.getState() != FishHook.HookState.BOBBING) {
                return;
            }

            Vector pull = anchor.clone().subtract(p.getEyeLocation().toVector());
            if (pull.lengthSquared() >= 1.0) {
                Vector velocity = pull.normalize().multiply(plugin.cfg().getGrapplingHookPullStrength());
                velocity.setY(Math.max(velocity.getY(), plugin.cfg().getGrapplingHookVerticalBoost()));
                p.setVelocity(velocity);
                p.getWorld().spawnParticle(Particle.CRIT, anchor.toLocation(p.getWorld()), 12, 0.15, 0.15, 0.15, 0.02);
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.7f, 1.2f);
            }

            hook.remove();
            task.cancel();
        }, null, 1L, 1L);
    }

    private long cooldownRemainingMs(UUID id) {
        Long until = cooldownUntil.get(id);
        if (until == null) return 0L;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            cooldownUntil.remove(id);
            return 0L;
        }
        return remaining;
    }
}
