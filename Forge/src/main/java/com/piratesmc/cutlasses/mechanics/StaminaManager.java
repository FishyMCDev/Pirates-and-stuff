package com.piratesmc.cutlasses.mechanics;

import com.piratesmc.cutlasses.CutlassVariant;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StaminaManager {

    public static class State {
        public int currentHits;
        public int maxHits;
        public int baseMaxHits;
        public boolean onCooldown;
        public long cooldownEndsAt;
        public long baseCooldownMs;
        public CutlassVariant lastVariant;

        public State(int max, CutlassVariant v) {
            this.currentHits = max;
            this.maxHits = max;
            this.baseMaxHits = max;
            this.lastVariant = v;
            this.onCooldown = false;
            this.cooldownEndsAt = 0L;
            this.baseCooldownMs = 0L;
        }
    }

    private final Map<UUID, Map<String, State>> states = new HashMap<>();
    private final Map<UUID, String> activeKeys = new HashMap<>();
    private final Map<UUID, Long> regenBoostUntil = new HashMap<>();
    private final Map<UUID, Double> regenCooldownMultipliers = new HashMap<>();
    private final Map<UUID, Integer> extraMaxHits = new HashMap<>();

    public State get(UUID id, String cutlassKey) {
        Map<String, State> playerStates = states.get(id);
        return playerStates == null ? null : playerStates.get(cutlassKey);
    }

    public State get(UUID id) {
        String activeKey = activeKeys.get(id);
        return activeKey == null ? null : get(id, activeKey);
    }

    
    public State ensure(UUID id, String cutlassKey, CutlassVariant variant, int maxHits) {
        activeKeys.put(id, cutlassKey);
        Map<String, State> playerStates = states.computeIfAbsent(id, ignored -> new HashMap<>());
        State s = playerStates.get(cutlassKey);
        if (s == null) {
            s = new State(maxHits, variant);
            playerStates.put(cutlassKey, s);
        } else if (s.lastVariant != variant || s.baseMaxHits != maxHits) {
            int oldBaseMax = s.baseMaxHits;
            s.baseMaxHits = maxHits;
            s.lastVariant = variant;
            if (maxHits > oldBaseMax && s.currentHits >= oldBaseMax) {
                s.currentHits = maxHits;
            } else {
                s.currentHits = Math.max(0, Math.min(maxHits, s.currentHits));
            }
        }
        refreshMaxHits(id, s);
        return s;
    }

    public State ensure(UUID id, CutlassVariant variant, int maxHits) {
        return ensure(id, variant.name(), variant, maxHits);
    }

    public void activate(UUID id, String cutlassKey) {
        activeKeys.put(id, cutlassKey);
    }

    public void consume(UUID id, int cooldownSeconds) {
        State s = get(id);
        consume(id, s, cooldownSeconds);
    }

    public void consume(UUID id, String cutlassKey, int cooldownSeconds) {
        activeKeys.put(id, cutlassKey);
        State s = get(id, cutlassKey);
        consume(id, s, cooldownSeconds);
    }

    private void consume(UUID id, State s, int cooldownSeconds) {
        if (s == null) return;
        refreshMaxHits(id, s);
        s.currentHits = Math.max(0, s.currentHits - 1);
        if (s.currentHits == 0) {
            s.onCooldown = true;
            long cooldownMs = Math.max(1L, Math.round(cooldownSeconds * 1000.0 * getActiveCooldownMultiplier(id)));
            s.baseCooldownMs = cooldownMs;
            s.cooldownEndsAt = System.currentTimeMillis() + cooldownMs;
        }
    }

    public void boostRegen(UUID id, int durationSeconds, double cooldownMultiplier, int extraHits, int restoreHits) {
        long now = System.currentTimeMillis();
        long until = now + Math.max(1, durationSeconds) * 1000L;
        double multiplier = Math.max(0.05, Math.min(1.0, cooldownMultiplier));
        regenBoostUntil.put(id, until);
        regenCooldownMultipliers.put(id, multiplier);
        extraMaxHits.put(id, Math.max(0, extraHits));

        Map<String, State> playerStates = states.get(id);
        if (playerStates != null) {
            for (State s : playerStates.values()) {
                if (s.onCooldown) {
                    long remaining = cooldownRemainingMs(s);
                    s.cooldownEndsAt = now + Math.max(1L, Math.round(remaining * multiplier));
                    s.baseCooldownMs = Math.max(1L, Math.round(s.baseCooldownMs * multiplier));
                }
                refreshMaxHits(id, s);
                s.currentHits = Math.max(0, Math.min(s.maxHits, s.currentHits + Math.max(0, restoreHits)));
            }
        }
    }

    
    public boolean tickCooldown(UUID id) {
        State s = get(id);
        return tickCooldown(id, s);
    }

    public boolean tickCooldown(UUID id, String cutlassKey) {
        activeKeys.put(id, cutlassKey);
        return tickCooldown(id, get(id, cutlassKey));
    }

    private boolean tickCooldown(UUID id, State s) {
        if (s == null || !s.onCooldown) return false;
        refreshMaxHits(id, s);
        if (System.currentTimeMillis() >= s.cooldownEndsAt) {
            s.onCooldown = false;
            s.currentHits = s.maxHits;
            s.cooldownEndsAt = 0;
            return true;
        }
        return false;
    }

    public boolean canBlock(UUID id) {
        State s = get(id);
        if (s != null) refreshMaxHits(id, s);
        return s != null && !s.onCooldown && s.currentHits > 0;
    }

    public boolean canBlock(UUID id, String cutlassKey) {
        activeKeys.put(id, cutlassKey);
        State s = get(id, cutlassKey);
        if (s != null) refreshMaxHits(id, s);
        return s != null && !s.onCooldown && s.currentHits > 0;
    }

    public long cooldownRemainingMs(UUID id) {
        return cooldownRemainingMs(get(id));
    }

    public long cooldownRemainingMs(UUID id, String cutlassKey) {
        activeKeys.put(id, cutlassKey);
        return cooldownRemainingMs(get(id, cutlassKey));
    }

    private long cooldownRemainingMs(State s) {
        if (s == null || !s.onCooldown) return 0;
        return Math.max(0, s.cooldownEndsAt - System.currentTimeMillis());
    }

    public long regenBoostRemainingMs(UUID id) {
        Long until = regenBoostUntil.get(id);
        if (until == null) return 0L;
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            regenBoostUntil.remove(id);
            regenCooldownMultipliers.remove(id);
            extraMaxHits.remove(id);
            Map<String, State> playerStates = states.get(id);
            if (playerStates != null) {
                for (State s : playerStates.values()) {
                    refreshMaxHits(id, s);
                }
            }
            return 0L;
        }
        return remaining;
    }

    public void clear(UUID id) {
        states.remove(id);
        activeKeys.remove(id);
        regenBoostUntil.remove(id);
        regenCooldownMultipliers.remove(id);
        extraMaxHits.remove(id);
    }

    public Map<UUID, Map<String, State>> all() {
        return states;
    }

    private double getActiveCooldownMultiplier(UUID id) {
        if (regenBoostRemainingMs(id) <= 0L) return 1.0;
        return regenCooldownMultipliers.getOrDefault(id, 1.0);
    }

    private void refreshMaxHits(UUID id, State s) {
        int previousMax = s.maxHits;
        int activeExtra = regenBoostRemainingMs(id) > 0L ? extraMaxHits.getOrDefault(id, 0) : 0;
        s.maxHits = Math.max(1, s.baseMaxHits + activeExtra);
        if (s.maxHits != previousMax) {
            int delta = s.maxHits - previousMax;
            s.currentHits = Math.max(0, Math.min(s.maxHits, s.currentHits + Math.max(0, delta)));
        } else {
            s.currentHits = Math.max(0, Math.min(s.maxHits, s.currentHits));
        }
    }
}
