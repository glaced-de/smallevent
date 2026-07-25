package net.glaced.desertjunglecore.command;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class VanishCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final Set<UUID> vanished = new HashSet<>();

    public VanishCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        UUID id = player.getUniqueId();

        // VANISH AUS
        if (vanished.contains(id)) {
            vanished.remove(id);

            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.equals(player)) continue;

                // EntityScheduler#execute(plugin, run, retired, delay)
                other.getScheduler().execute(
                        plugin,
                        () -> other.showPlayer(plugin, player),
                        null,
                        0L
                );
            }

            player.sendMessage("§cVanish disabled.");
            return true;
        }

        // VANISH AN
        vanished.add(id);

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.equals(player)) continue;

            other.getScheduler().execute(
                    plugin,
                    () -> other.hidePlayer(plugin, player),
                    null,
                    0L
            );
        }

        player.sendMessage("§aVanish enabled.");

        // Actionbar-Reminder (Folia-safe, GlobalRegionScheduler)
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                (ScheduledTask task) -> {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null || !p.isOnline() || !vanished.contains(id)) {
                        task.cancel();
                        return;
                    }

                    p.getScheduler().execute(
                            plugin,
                            () -> p.sendActionBar("§c§lVANISH!"),
                            null,
                            0L
                    );
                },
                20L,
                20L
        );

        return true;
    }
}
