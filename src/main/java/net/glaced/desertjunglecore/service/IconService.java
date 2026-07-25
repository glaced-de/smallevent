package net.glaced.desertjunglecore.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores each player's chosen HUD icon in memory. Thread-safe for Folia's per-region access. */
public final class IconService {

    private static final String DEFAULT_ICON = "\u2726"; // fallback glyph, adjust to your icon set

    private final Map<UUID, String> icons = new ConcurrentHashMap<>();

    public String get(UUID playerId) {
        return icons.getOrDefault(playerId, DEFAULT_ICON);
    }

    public void set(UUID playerId, String icon) {
        icons.put(playerId, icon);
    }

    public void remove(UUID playerId) {
        icons.remove(playerId);
    }
}