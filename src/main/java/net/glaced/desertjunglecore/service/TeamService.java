package net.glaced.desertjunglecore.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.glaced.desertjunglecore.api.EventTeam;
import net.glaced.desertjunglecore.config.PluginConfiguration;
import net.glaced.desertjunglecore.data.PlayerData;
import net.glaced.desertjunglecore.data.SqliteRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Thread-safe team projection with O(1) team-size reads for the HUD. */
public final class TeamService {
    private final JavaPlugin plugin;
    private final SqliteRepository repository;
    private final PluginConfiguration config;
    private final SpawnService spawns;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final Map<EventTeam, AtomicInteger> sizes = Map.of(
        EventTeam.DESERT, new AtomicInteger(), EventTeam.JUNGLE, new AtomicInteger()
    );

    public TeamService(JavaPlugin plugin, SqliteRepository repository, PluginConfiguration config, SpawnService spawns) {
        this.plugin = plugin;
        this.repository = repository;
        this.config = config;
        this.spawns = spawns;
    }

    public void loadOnlinePlayers() { Bukkit.getOnlinePlayers().forEach(this::load); }

    public void load(Player player) {
        repository.load(player.getUniqueId()).whenComplete((data, error) -> player.getScheduler().run(plugin, ignored -> {
            if (error != null) { plugin.getLogger().warning("Could not load " + player.getName()); return; }
            PlayerData previous = players.put(data.id(), data);
            if (previous != null && previous.team() != null) sizes.get(previous.team()).decrementAndGet();
            if (data.team() != null) sizes.get(data.team()).incrementAndGet();
        }, null));
    }

    public EventTeam teamOf(UUID id) { PlayerData data = players.get(id); return data == null ? null : data.team(); }
    public int size(EventTeam team) { return sizes.get(team).get(); }

    public boolean join(Player player, EventTeam team) {
        if (teamOf(player.getUniqueId()) == team) return true;
        if (size(team) >= config.maxTeamSize()) return false;
        setTeam(player, team);
        teleportToSpawn(player);
        return true;
    }

    public void setTeam(Player player, EventTeam team) {
        PlayerData old = players.getOrDefault(player.getUniqueId(), PlayerData.empty(player.getUniqueId()));
        if (old.team() != null) sizes.get(old.team()).decrementAndGet();
        sizes.get(team).incrementAndGet();
        PlayerData next = new PlayerData(old.id(), team, old.group(), old.kills(), old.deaths(), old.assists());
        players.put(next.id(), next);
        repository.save(next);
        player.sendMessage(Component.text("Team set to " + team.display(), team.color()));
    }

    public void leave(Player player) {
        PlayerData old = players.getOrDefault(player.getUniqueId(), PlayerData.empty(player.getUniqueId()));
        if (old.team() != null) sizes.get(old.team()).decrementAndGet();
        PlayerData next = new PlayerData(old.id(), null, old.group(), old.kills(), old.deaths(), old.assists());
        players.put(next.id(), next);
        repository.save(next);
    }

    public boolean teleportToSpawn(Player player) {
        EventTeam team = teamOf(player.getUniqueId());
        if (team == null) return false;
        Location spawn = spawns.select(team, player.getUniqueId());
        if (spawn == null) return false;
        player.teleportAsync(spawn);
        return true;
    }

    public void teleportAllToSpawns() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(plugin, ignored -> teleportToSpawn(player), null);
        }
    }

    public Location respawnLocation(Player player) {
        EventTeam team = teamOf(player.getUniqueId());
        return team == null ? null : spawns.select(team, player.getUniqueId());
    }

    public void kill(UUID id) { change(id, 1, 0, 0); }
    public void death(UUID id) { change(id, 0, 1, 0); }
    public int kills(UUID id) { return players.getOrDefault(id, PlayerData.empty(id)).kills(); }
    public int deaths(UUID id) { return players.getOrDefault(id, PlayerData.empty(id)).deaths(); }

    private void change(UUID id, int kills, int deaths, int assists) {
        PlayerData old = players.getOrDefault(id, PlayerData.empty(id));
        PlayerData next = new PlayerData(id, old.team(), old.group(), old.kills() + kills, old.deaths() + deaths, old.assists() + assists);
        players.put(id, next);
        repository.save(next);
    }
}
