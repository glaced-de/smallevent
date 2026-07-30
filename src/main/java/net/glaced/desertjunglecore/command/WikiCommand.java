package net.glaced.desertjunglecore.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class WikiCommand implements CommandExecutor {
    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        sender.sendMessage("§e§lWIKI");
        sender.sendMessage("§7━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§fChange your team with:");
        sender.sendMessage("§e/team leave");
        sender.sendMessage("§e/team join team1 §7or §e/team join team2");
        sender.sendMessage("§7━━━━━━━━━━━━━━━━━━━━");

        return true;
    }
}
