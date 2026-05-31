package com.piratesmc.cutlasses.listeners;

import com.piratesmc.cutlasses.CutlassesPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class MovementListener implements Listener {

    private final CutlassesPlugin plugin;

    public MovementListener(CutlassesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        double dx = e.getTo().getX() - e.getFrom().getX();
        double dz = e.getTo().getZ() - e.getFrom().getZ();

        double lenSq = dx * dx + dz * dz;
        if (lenSq < 0.0001) return;

        double len = Math.sqrt(lenSq);
        Vector dir = new Vector(dx / len, 0, dz / len);

        Player p = e.getPlayer();
        plugin.dodge().setLastMoveDirection(p.getUniqueId(), dir);
    }
}
