package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassVariant;
import com.piratesmc.cutlasses.CutlassesPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class DodgeListener implements Listener {

    private final CutlassesPlugin plugin;

    public DodgeListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        if (!e.isSneaking()) return;
        tryDodge(e.getPlayer());
    }

    public boolean tryDodge(Player p) {
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        CutlassVariant variant = plugin.cutlasses().getVariant(mainHand);
        if (variant == null) return false;
        String staminaKey = plugin.cutlasses().getStaminaKey(mainHand);
        if (staminaKey == null) return false;

        boolean isBlocking = plugin.blocks().isBlocking(p.getUniqueId()) || p.isHandRaised();
        if (!isBlocking) return false;

        if (plugin.dodge().isOnCooldown(p.getUniqueId())) {
            p.sendActionBar(Component.text("\u00a77Dodge on cooldown..."));
            plugin.bar().update(p, mainHand, variant);
            return false;
        }
        if (!plugin.stamina().canBlock(p.getUniqueId(), staminaKey)) {
            p.sendActionBar(Component.text("\u00a7cNo stamina to dodge!"));
            plugin.bar().update(p, mainHand, variant);
            return false;
        }

        Vector direction = resolveDirection(p);
        plugin.blocks().setBlocking(p.getUniqueId(), false);
        p.clearActiveItem();
        p.setCooldown(mainHand, Math.max(1, plugin.cfg().getDashShieldDisableSeconds() * 20));

        double mult = plugin.cfg().getDodgeVelocityMultiplier();
        double upward = plugin.cfg().getDodgeVerticalBoost();
        Vector dodge = direction.multiply(mult).setY(upward);

        p.setVelocity(dodge);
        p.setNoDamageTicks(plugin.cfg().getDodgeIFramesTicks());

        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 20, 0.4, 0.1, 0.4, 0.02);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.8f, 1.4f);

        plugin.dodge().startCooldown(p.getUniqueId(), plugin.cfg().getDodgeCooldownSeconds());
        plugin.blocks().dashCooldownFor(p.getUniqueId(), plugin.cfg().getDashShieldDisableSeconds());
        plugin.stamina().consume(p.getUniqueId(), staminaKey, plugin.cfg().getStaminaCooldownSeconds());
        plugin.bar().update(p, mainHand, variant);
        return true;
    }

    private Vector resolveDirection(Player p) {
        Vector tracked = plugin.dodge().getLastMoveDirection(p.getUniqueId());
        if (tracked != null && tracked.lengthSquared() > 0.001) {
            return tracked;
        }

        Vector velocity = p.getVelocity().clone().setY(0);
        if (velocity.lengthSquared() > 0.05 * 0.05) {
            return velocity.normalize();
        }

        Vector look = p.getLocation().getDirection().setY(0);
        if (look.lengthSquared() > 0.001) {
            return look.normalize().multiply(-1);
        }

        double yaw = Math.toRadians(p.getLocation().getYaw());
        return new Vector(Math.sin(yaw), 0, -Math.cos(yaw)).normalize();
    }
}
