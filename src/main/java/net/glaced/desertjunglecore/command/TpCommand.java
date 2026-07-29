package net.glaced.desertjunglecore.command;

import net.glaced.desertjunglecore.DesertJungleCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * /tp <player>            - teleports the sender to the given player.
 * /tp <player> <target>   - teleports <player> to <target>.
 */
public final class TpCommand implements CommandExecutor, TabCompleter {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                sender.sendMessage(Component.text("Nur Spieler können diesen Befehl ohne Ziel-Spieler nutzen.", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(Component.text("Spieler '" + args[0] + "' ist nicht online.", NamedTextColor.RED));
                return true;
            }

            self.teleportAsync(target.getLocation());
            self.sendMessage(Component.text("Du wurdest zu " + target.getName() + " teleportiert.", NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 2) {
            if (!sender.hasPermission("desertjunglecore.tp.others")) {
                sender.sendMessage(Component.text("Dir fehlt die Berechtigung, andere Spieler zu teleportieren.", NamedTextColor.RED));
                return true;
            }

            Player player = Bukkit.getPlayerExact(args[0]);
            Player target = Bukkit.getPlayerExact(args[1]);

            if (player == null) {
                sender.sendMessage(Component.text("Spieler '" + args[0] + "' ist nicht online.", NamedTextColor.RED));
                return true;
            }
            if (target == null) {
                sender.sendMessage(Component.text("Spieler '" + args[1] + "' ist nicht online.", NamedTextColor.RED));
                return true;
            }

            player.teleportAsync(target.getLocation());
            sender.sendMessage(Component.text(player.getName() + " wurde zu " + target.getName() + " teleportiert.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Verwendung: /tp <Spieler> [Ziel]", NamedTextColor.RED));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1 || args.length == 2) {
            String partial = args[args.length - 1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}