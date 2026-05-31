package com.piratesmc.cutlasses.mechanics;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockHandler {

    private final Map<UUID, Boolean> blocking = new HashMap<>();
    private final Map<UUID, Long> disabledUntil = new HashMap<>();
    private final Map<UUID, Long> cooldownUntil = new HashMap<>();
    private final Map<UUID, Long> dashCooldownUntil = new HashMap<>();
    private final Map<UUID, Long> lastRightClickMs = new HashMap<>();
    private final Map<UUID, Long> blockStartedMs = new HashMap<>();

    public boolean isBlocking(UUID id) {
        Boolean v = blocking.get(id);
        return v != null && v;
    }

    public void setBlocking(UUID id, boolean state) {
        if (state) {
            blocking.put(id, true);
            blockStartedMs.putIfAbsent(id, System.currentTimeMillis());
        } else {
            blocking.remove(id);
            blockStartedMs.remove(id);
        }
    }

    public void refreshRightClick(UUID id) {
        lastRightClickMs.put(id, System.currentTimeMillis());
    }

    public long getLastRightClickMs(UUID id) {
        Long t = lastRightClickMs.get(id);
        return t == null ? 0L : t;
    }

    public long getBlockStartedMs(UUID id) {
        Long t = blockStartedMs.get(id);
        return t == null ? 0L : t;
    }

    public boolean isDisabled(UUID id) {
        Long until = disabledUntil.get(id);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            disabledUntil.remove(id);
            return false;
        }
        return true;
    }

    public boolean isCoolingDown(UUID id) {
        Long until = cooldownUntil.get(id);
        if (until != null && System.currentTimeMillis() < until) return true;
        
        until = dashCooldownUntil.get(id);
        if (until != null && System.currentTimeMillis() < until) return true;
        
        return false;
    }

    public boolean isOnDashCooldown(UUID id) {
        Long until = dashCooldownUntil.get(id);
        return until != null && System.currentTimeMillis() < until;
    }

    public void dashCooldownFor(UUID id, int seconds) {
        dashCooldownUntil.put(id, System.currentTimeMillis() + seconds * 1000L);
        blocking.remove(id);
        blockStartedMs.remove(id);
    }

    public void disableFor(UUID id, int seconds) {
        disabledUntil.put(id, System.currentTimeMillis() + seconds * 1000L);
        blocking.remove(id);
        blockStartedMs.remove(id);
    }

    public void cooldownFor(UUID id, int seconds) {
        cooldownUntil.put(id, System.currentTimeMillis() + seconds * 1000L);
        blocking.remove(id);
        blockStartedMs.remove(id);
    }

    public long disabledRemainingMs(UUID id) {
        Long until = disabledUntil.get(id);
        if (until == null) return 0;
        long diff = until - System.currentTimeMillis();
        return Math.max(0, diff);
    }

    public long cooldownRemainingMs(UUID id) {
        Long until = cooldownUntil.get(id);
        long diff = 0;
        if (until != null) {
            diff = Math.max(diff, until - System.currentTimeMillis());
        }
        until = dashCooldownUntil.get(id);
        if (until != null) {
            diff = Math.max(diff, until - System.currentTimeMillis());
        }
        return Math.max(0, diff);
    }

    public void clear(UUID id) {
        blocking.remove(id);
        disabledUntil.remove(id);
        cooldownUntil.remove(id);
        dashCooldownUntil.remove(id);
        lastRightClickMs.remove(id);
        blockStartedMs.remove(id);
    }
}
