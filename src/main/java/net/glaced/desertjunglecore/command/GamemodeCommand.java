package net.glaced.desertjunglecore.command;

import net.glaced.desertjunglecore.service.MessageService;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class GamemodeCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public GamemodeCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            MessageService.send(player, "§cUsage: /gm <0-3>");
            return true;
        }

        GameMode mode;

        switch (args[0]) {
            case "0", "s", "survival" -> mode = GameMode.SURVIVAL;
            case "1", "c", "creative" -> mode = GameMode.CREATIVE;
            case "2", "a", "adventure" -> mode = GameMode.ADVENTURE;
            case "3", "sp", "spectator" -> mode = GameMode.SPECTATOR;
            default -> {
                player.sendActionBar("§cInvalid gamemode: " + args[0]);
                return true;
            }
        }

        // Folia-safe Gamemode change
        player.getScheduler().execute(
                plugin,
                () -> {
                    player.setGameMode(mode);
                    player.sendActionBar("§eGamemode changed to §6" + mode.name());
                },
                null,
                0L
        );
        return true;
    }
}
