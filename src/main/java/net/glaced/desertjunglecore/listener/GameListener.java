package net.glaced.desertjunglecore.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.glaced.desertjunglecore.api.EventTeam;
import net.glaced.desertjunglecore.event.EventService;
import net.glaced.desertjunglecore.service.MuteService;
import net.glaced.desertjunglecore.service.TeamService;
import net.glaced.desertjunglecore.ui.HudService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Gameplay listeners with no blocking I/O on region threads. */
public final class GameListener implements Listener {
    private final JavaPlugin plugin;
    private final TeamService teams;
    private final HudService hud;
    private final MuteService muteService;


    // Merkt sich die Todesposition, damit beim Respawn nicht zum Weltspawn teleportiert wird.
    private final Map<UUID, Location> deathLocations = new ConcurrentHashMap<>();

    public GameListener(JavaPlugin plugin, TeamService teams, EventService ignoredEvent, HudService hud, MuteService muteService) {
        this.plugin = plugin;
        this.teams = teams;
        this.hud = hud;
        this.muteService = muteService;
    }

    @EventHandler public void join(PlayerJoinEvent event) {
        teams.load(event.getPlayer());
        hud.initialize(event.getPlayer());

        Player player = event.getPlayer();
        String name = player.getName();
        event.setJoinMessage("§a[+] " + name);
    }

    @EventHandler public void quit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String name = player.getName();

        hud.remove(event.getPlayer());
        deathLocations.remove(player.getUniqueId());

        event.setQuitMessage("§c[-] " + name);
    }

    @EventHandler(ignoreCancelled = true) public void damage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) return;
        EventTeam attackerTeam = teams.teamOf(attacker.getUniqueId());
        EventTeam victimTeam = teams.teamOf(victim.getUniqueId());
        if (attackerTeam != null && attackerTeam == victimTeam && !plugin.getConfig().getBoolean("teams.friendly-fire", false)) event.setCancelled(true);
    }

    @EventHandler public void death(PlayerDeathEvent event) {
        Player victim = event.getPlayer();
        teams.death(victim.getUniqueId());
        if (victim.getKiller() != null) teams.kill(victim.getKiller().getUniqueId());

        // Todesort merken, damit der Spieler dort als Spectator bleibt statt zum Spawn zu teleportieren.
        deathLocations.put(victim.getUniqueId(), victim.getLocation().clone());

        // Eliminate on death: no more team respawns for this life, they become a spectator.
        teams.leave(victim);
    }

    @EventHandler public void respawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        EventTeam team = teams.teamOf(player.getUniqueId());
        if (team != null) {
            Location spawn = teams.respawnLocation(player);
            if (spawn != null) event.setRespawnLocation(spawn);
            return;
        }

        // Kein Team mehr (gerade eliminiert oder nie beigetreten): kein Teleport zum Weltspawn,
        // stattdessen an der Todesposition bleiben und direkt Spectator werden.
        Location deathLoc = deathLocations.remove(player.getUniqueId());
        if (deathLoc != null) {
            event.setRespawnLocation(deathLoc);
        }

        player.getScheduler().run(plugin, ignored -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("You have been eliminated and are now spectating.", NamedTextColor.GRAY));
        }, null);
    }

    @EventHandler public void chat(AsyncChatEvent event) {
        EventTeam team = teams.teamOf(event.getPlayer().getUniqueId());
        event.renderer((source, name, message, viewer) -> Component.text()
                .append(team == null ? Component.empty() : Component.text(team.display() + " ", team.color()))
                .append(name.color(NamedTextColor.WHITE))
                .append(Component.text(" > ", NamedTextColor.DARK_GRAY))
                .append(message)
                .build());

        UUID id = event.getPlayer().getUniqueId();

        if (muteService.isMuted(id)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou are muted.");
        }
    }
}