package com.piratesmc.cutlasses.mechanics;

import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DodgeHandler {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Vector> lastMoveDirection = new HashMap<>();

    public boolean isOnCooldown(UUID id) {
        Long until = cooldowns.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            cooldowns.remove(id);
            return false;
        }
        return true;
    }

    public void startCooldown(UUID id, double seconds) {
        cooldowns.put(id, System.currentTimeMillis() + (long)(seconds * 1000));
    }

    public long cooldownRemainingMs(UUID id) {
        Long until = cooldowns.get(id);
        if (until == null) return 0L;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            cooldowns.remove(id);
            return 0L;
        }
        return remaining;
    }

    
    public void setLastMoveDirection(UUID id, Vector normalised) {
        lastMoveDirection.put(id, normalised.clone());
    }

    
    public Vector getLastMoveDirection(UUID id) {
        Vector v = lastMoveDirection.get(id);
        return v == null ? null : v.clone();
    }

    public void clear(UUID id) {
        cooldowns.remove(id);
        lastMoveDirection.remove(id);
    }
}
