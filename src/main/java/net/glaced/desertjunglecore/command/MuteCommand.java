package net.glaced.desertjunglecore.command;

import net.glaced.desertjunglecore.service.MessageService;
import net.glaced.desertjunglecore.service.MuteService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class MuteCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MuteService muteService;

    public MuteCommand(JavaPlugin plugin, MuteService muteService) {
        this.plugin = plugin;
        this.muteService = muteService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!(sender instanceof Player admin)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            MessageService.send(admin, "§cUsage /mute <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            MessageService.send(admin, "§cPlayer: " + target.getName() + " not found");
            return true;
        }

        UUID id = target.getUniqueId();

        // UNMUTE
        if (muteService.isMuted(id)) {
            muteService.unmute(id);

            admin.getScheduler().execute(
                    plugin,
                    () -> admin.sendActionBar("§aYou unmuted §e" + target.getName()),
                    null,
                    0L
            );
            return true;
        }

        // MUTE
        muteService.mute(id);

        admin.getScheduler().execute(
                plugin,
                () -> admin.sendActionBar("§cYou muted §e" + target.getName()),
                null,
                0L
        );
        return true;
    }
}
