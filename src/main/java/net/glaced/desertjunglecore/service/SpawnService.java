package net.glaced.desertjunglecore.service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.glaced.desertjunglecore.api.EventTeam;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

/** Stores up to fifty dedicated spawn slots per event team in config.yml. */
public final class SpawnService {
    public static final int MAX_SPAWNS_PER_TEAM = 50;
    private final JavaPlugin plugin;
    private final Map<EventTeam, Map<Integer, Location>> spawns = new EnumMap<>(EventTeam.class);

    public SpawnService(JavaPlugin plugin) {
        this.plugin = plugin;
        for (EventTeam team : EventTeam.values()) spawns.put(team, new ConcurrentHashMap<>());
        load();
    }

    private void load() {
        for (EventTeam team : EventTeam.values()) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("team-spawns." + team.name().toLowerCase());
            if (section == null) continue;
            for (String key : section.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    Location location = section.getLocation(key);
                    if (slot >= 1 && slot <= MAX_SPAWNS_PER_TEAM && location != null) spawns.get(team).put(slot, location);
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    public void set(EventTeam team, int slot, Location location) {
        validateSlot(slot);
        Location saved = location.clone();
        spawns.get(team).put(slot, saved);
        plugin.getConfig().set("team-spawns." + team.name().toLowerCase() + "." + slot, saved);
        plugin.saveConfig();
    }

    public Location select(EventTeam team, UUID playerId) {
        List<Integer> slots = new ArrayList<>(spawns.get(team).keySet());
        if (slots.isEmpty()) return null;
        slots.sort(Integer::compareTo);
        int selected = Math.floorMod(playerId.hashCode(), slots.size());
        return spawns.get(team).get(slots.get(selected)).clone();
    }

    public int count(EventTeam team) { return spawns.get(team).size(); }

    private void validateSlot(int slot) {
        if (slot < 1 || slot > MAX_SPAWNS_PER_TEAM) throw new IllegalArgumentException("Spawn slot must be between 1 and 50.");
    }
}
