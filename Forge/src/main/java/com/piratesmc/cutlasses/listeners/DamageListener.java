package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DamageListener implements Listener {

    private final CutlassesPlugin plugin;

    public DamageListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player defender)) return;
        if (!plugin.blocks().isBlocking(defender.getUniqueId())) return;

        ItemStack mainHand = defender.getInventory().getItemInMainHand();
        CutlassVariant variant = plugin.cutlasses().getVariant(mainHand);
        if (variant == null) {
            plugin.blocks().setBlocking(defender.getUniqueId(), false);
            plugin.bar().hide(defender);
            return;
        }
        String staminaKey = plugin.cutlasses().getStaminaKey(mainHand);
        if (staminaKey == null) return;

        if (plugin.blocks().isDisabled(defender.getUniqueId())
                || plugin.blocks().isCoolingDown(defender.getUniqueId())) {
            applyShieldCooldown(defender);
            return;
        }
        if (!plugin.stamina().canBlock(defender.getUniqueId(), staminaKey)) {
            plugin.blocks().setBlocking(defender.getUniqueId(), false);
            applyStaminaCooldown(defender, staminaKey);
            plugin.bar().update(defender, mainHand, variant);
            return;
        }

        LivingEntity attackerLiving = null;
        if (e.getDamager() instanceof LivingEntity le) {
            attackerLiving = le;
        } else if (e.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof LivingEntity le) {
            attackerLiving = le;
        }

        if (e.getDamager() instanceof Projectile projectile) {
            if (!isProjectileInFront(defender, projectile)) {
                return;
            }
        } else if (attackerLiving != null) {
            if (!isSourceInFront(defender, attackerLiving.getLocation())) {
                return;
            }
        } else {
            return;
        }

        if (attackerLiving instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon != null && Tag.ITEMS_AXES.isTagged(weapon.getType())) {
                plugin.blocks().disableFor(defender.getUniqueId(),
                        plugin.cfg().getAxeDisableSeconds());
                applyShieldCooldown(defender);
                defender.getWorld().playSound(defender.getLocation(),
                        Sound.ITEM_SHIELD_BREAK, 1.0f, 1.0f);
                plugin.bar().update(defender, mainHand, variant);
                return;
            }
        }

        e.setCancelled(true);
        plugin.stamina().consume(defender.getUniqueId(), staminaKey,
                plugin.cfg().getStaminaCooldownSeconds());
        defender.getWorld().playSound(defender.getLocation(),
                Sound.ITEM_SHIELD_BLOCK, 0.9f, 1.0f);

        if (attackerLiving != null && plugin.cfg().isAttackerKnockbackEnabled()) {
            Vector dir = attackerLiving.getLocation().toVector()
                    .subtract(defender.getLocation().toVector());
            dir.setY(0);
            if (dir.lengthSquared() > 0.0001) {
                dir.normalize().multiply(plugin.cfg().getAttackerKnockback());
                dir.setY(0.25);
                try {
                    attackerLiving.setVelocity(dir);
                } catch (Throwable ignored) {
                }
            }
        }

        if (!plugin.stamina().canBlock(defender.getUniqueId(), staminaKey)) {
            plugin.blocks().setBlocking(defender.getUniqueId(), false);
            applyStaminaCooldown(defender, staminaKey);
        }

        plugin.bar().update(defender, mainHand, variant);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamagingPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker) || !(e.getEntity() instanceof Player)) return;
        if (!plugin.cfg().isPlayerHitSoundEnabled()) return;
        if (e.getFinalDamage() <= 0.0) return;
        if (!plugin.cutlasses().isCutlass(attacker.getInventory().getItemInMainHand())) return;

        attacker.playSound(attacker.getLocation(), plugin.cfg().getPlayerHitSound(), 1.0f, 1.0f);
    }

    private boolean isSourceInFront(Player defender, Location source) {
        Vector facing = defender.getLocation().getDirection().setY(0);
        Vector toSource = source.toVector()
                .subtract(defender.getLocation().toVector()).setY(0);

        if (facing.lengthSquared() < 0.001 || toSource.lengthSquared() < 0.001) {
            return true;
        }

        double dot = facing.normalize().dot(toSource.normalize());
        return dot > 0.0;
    }

    private boolean isProjectileInFront(Player defender, Projectile projectile) {
        Vector facing = defender.getLocation().getDirection().setY(0);
        Vector travel = projectile.getVelocity().clone().setY(0);

        if (facing.lengthSquared() < 0.001 || travel.lengthSquared() < 0.001) {
            return isSourceInFront(defender, projectile.getLocation());
        }

        double dot = facing.normalize().dot(travel.normalize());
        return dot < 0.0;
    }

    private void applyShieldCooldown(Player player) {
        long remaining = Math.max(plugin.blocks().disabledRemainingMs(player.getUniqueId()),
                plugin.blocks().cooldownRemainingMs(player.getUniqueId()));
        int ticks = Math.max(1, (int) Math.ceil(remaining / 50.0));
        player.setCooldown(player.getInventory().getItemInMainHand(), ticks);
    }

    private void applyStaminaCooldown(Player player, String staminaKey) {
        long remaining = plugin.stamina().cooldownRemainingMs(player.getUniqueId(), staminaKey);
        int ticks = Math.max(1, (int) Math.ceil(remaining / 50.0));
        player.setCooldown(player.getInventory().getItemInMainHand(), ticks);
    }
}
