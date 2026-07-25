package net.glaced.desertjunglecore.command;

import net.glaced.desertjunglecore.service.MessageService;
import net.glaced.desertjunglecore.service.MuteService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeedCommand implements CommandExecutor {
    private final JavaPlugin plugin;

    public SpeedCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length != 1) {
            MessageService.send( player, "§cError usage: /speed <1-50>");
            return true;
        }

        int value;
        try {
            value = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            MessageService.send( player, "§cError usage: /speed <1-50>");
            return true;
        }

        if (value < 1 || value > 50) {
            MessageService.send( player, "§cError usage: /speed <1-50>");
            return true;
        }

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            MessageService.send( player, "§cError you must be in creative or spectator mode to apply speed");
            return true;
        }

        float flySpeed = value / 50f;
        player.setFlySpeed(flySpeed);
        player.sendActionBar("§aFly speed set to " + value + "!");
        return true;
    }
}

