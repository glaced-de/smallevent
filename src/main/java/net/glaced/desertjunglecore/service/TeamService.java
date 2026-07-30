package net.glaced.desertjunglecore.service;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.glaced.desertjunglecore.api.EventTeam;
import net.glaced.desertjunglecore.config.PluginConfiguration;
import net.glaced.desertjunglecore.data.PlayerData;
import net.glaced.desertjunglecore.data.SqliteRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Thread-safe team projection with O(1) team-size reads for the HUD. */
public final class TeamService {
    private final JavaPlugin plugin;
    private LuckPerms luckPerms;
    private final SqliteRepository repository;
    private final PluginConfiguration config;
    private final SpawnService spawns;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final Map<EventTeam, AtomicInteger> sizes = Map.of(
            EventTeam.TEAM1, new AtomicInteger(), EventTeam.TEAM2, new AtomicInteger()
    );
    private final Random random = new Random();

    public TeamService(JavaPlugin plugin, SqliteRepository repository, PluginConfiguration config, SpawnService spawns) {
        this.plugin = plugin;
        this.repository = repository;
        this.config = config;
        this.spawns = spawns;
    }

    /** Must be called once during plugin bootstrap, e.g. teamService.setLuckPerms(LuckPermsProvider.get()). */
    public void setLuckPerms(LuckPerms luckPerms) {
        this.luckPerms = luckPerms;
    }

    public void loadOnlinePlayers() { Bukkit.getOnlinePlayers().forEach(this::load); }

    public void load(Player player) {
        repository.load(player.getUniqueId()).whenComplete((loaded, error) -> player.getScheduler().run(plugin, ignored -> {
            if (error != null) { plugin.getLogger().warning("Could not load " + player.getName()); return; }

            PlayerData data = loaded;
            if (data.team() == null && data.firstAssignedAt() == 0L && config.autoAssignTeamOnJoin()) {
                EventTeam randomTeam = pickRandomTeam();
                PlayerData assigned = new PlayerData(data.id(), randomTeam, data.group(), data.kills(), data.deaths(), data.assists(), System.currentTimeMillis());
                repository.save(assigned);
                applyLuckPermsGroup(player, null, randomTeam);
                player.sendMessage(Component.text("You are randomly choosed for " + randomTeam.display() + ". If you want to change that /wiki", randomTeam.color()));
                player.sendActionBar("§e§lYou are §7" + randomTeam.display() + "/wiki if you want to change");
                data = assigned;
            }

            PlayerData toStore = data;
            PlayerData previous = players.put(toStore.id(), toStore);
            if (previous != null && previous.team() != null) sizes.get(previous.team()).decrementAndGet();
            if (toStore.team() != null) sizes.get(toStore.team()).incrementAndGet();
        }, null));
    }

    /** Wählt zufällig ein Team, weicht aber auf das andere aus, falls das ausgeloste bereits voll ist. */
    private EventTeam pickRandomTeam() {
        EventTeam choice = random.nextBoolean() ? EventTeam.TEAM1 : EventTeam.TEAM2;
        if (size(choice) >= config.maxTeamSize()) {
            EventTeam other = choice == EventTeam.TEAM1 ? EventTeam.TEAM2 : EventTeam.TEAM1;
            if (size(other) < config.maxTeamSize()) return other;
        }
        return choice;
    }

    public EventTeam teamOf(UUID id) { PlayerData data = players.get(id); return data == null ? null : data.team(); }
    public int size(EventTeam team) { return sizes.get(team).get(); }

    public boolean join(Player player, EventTeam team) {
        if (teamOf(player.getUniqueId()) == team) return true;
        if (size(team) >= config.maxTeamSize()) return false;

        if (!canSwitchTeam(player.getUniqueId())) {
            player.sendMessage(Component.text("Team-change is only possible on day 1.", NamedTextColor.RED));
            return false;
        }

        setTeam(player, team);
        teleportToSpawn(player);
        return true;
    }

    /** True, wenn der Spieler sein Team noch wechseln darf (Config-abhängig und zeitfensterabhängig). */
    private boolean canSwitchTeam(UUID id) {
        if (!config.restrictTeamSwitchToFirstDay()) return true;
        PlayerData data = players.get(id);
        if (data == null || data.firstAssignedAt() == 0L) return true;
        long elapsed = System.currentTimeMillis() - data.firstAssignedAt();
        return elapsed <= config.teamSwitchWindowMillis();
    }

    public void setTeam(Player player, EventTeam team) {
        PlayerData old = players.getOrDefault(player.getUniqueId(), PlayerData.empty(player.getUniqueId()));
        if (old.team() != null) sizes.get(old.team()).decrementAndGet();
        sizes.get(team).incrementAndGet();

        long firstAssignedAt = old.firstAssignedAt() == 0L ? System.currentTimeMillis() : old.firstAssignedAt();
        PlayerData next = new PlayerData(old.id(), team, old.group(), old.kills(), old.deaths(), old.assists(), firstAssignedAt);
        players.put(next.id(), next);
        repository.save(next);
        player.sendMessage(Component.text("Team set to " + team.display(), team.color()));

        applyLuckPermsGroup(player, old.team(), team);
    }

    /**
     * Aktualisiert die LuckPerms-Gruppe eines Spielers.
     * newTeam darf null sein (z. B. beim Team-Leave) — dann wird nur die alte Gruppe entfernt,
     * ohne eine neue hinzuzufügen.
     */
    private void applyLuckPermsGroup(Player player, EventTeam previousTeam, EventTeam newTeam) {
        if (luckPerms == null) {
            plugin.getLogger().warning("LuckPerms is not initialized on TeamService; skipping group update for " + player.getName());
            return;
        }
        luckPerms.getUserManager().loadUser(player.getUniqueId()).thenAcceptAsync(user -> {
            if (previousTeam != null && previousTeam != newTeam) {
                user.data().remove(Node.builder("group." + previousTeam.luckPermsGroup()).build());
            }
            if (newTeam != null) {
                user.data().add(Node.builder("group." + newTeam.luckPermsGroup()).build());
            }
            luckPerms.getUserManager().saveUser(user);
        }).exceptionally(error -> {
            plugin.getLogger().warning("Could not update LuckPerms group for " + player.getName() + ": " + error.getMessage());
            return null;
        });
    }

    public void leave(Player player) {
        PlayerData old = players.getOrDefault(player.getUniqueId(), PlayerData.empty(player.getUniqueId()));
        if (old.team() != null) sizes.get(old.team()).decrementAndGet();
        PlayerData next = new PlayerData(old.id(), null, old.group(), old.kills(), old.deaths(), old.assists(), old.firstAssignedAt());
        players.put(next.id(), next);
        repository.save(next);
        applyLuckPermsGroup(player, old.team(), null);
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
        PlayerData next = new PlayerData(id, old.team(), old.group(), old.kills() + kills, old.deaths() + deaths, old.assists() + assists, old.firstAssignedAt());
        players.put(id, next);
        repository.save(next);
    }
}