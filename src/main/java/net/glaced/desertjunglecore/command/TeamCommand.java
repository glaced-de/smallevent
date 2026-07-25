package net.glaced.desertjunglecore.command;

import java.util.List;
import net.glaced.desertjunglecore.api.EventTeam;
import net.glaced.desertjunglecore.service.SpawnService;
import net.glaced.desertjunglecore.service.TeamService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private final TeamService teams;
    private final SpawnService spawns;
    public TeamCommand(TeamService teams, SpawnService spawns) { this.teams = teams; this.spawns = spawns; }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Players only."); return true; }
        if (args.length == 0) { player.sendMessage("/team <desert|jungle|leave|spawn>"); return true; }
        if (args[0].equalsIgnoreCase("spawn")) return spawnCommand(player, args);
        if (args[0].equalsIgnoreCase("set") && args.length == 3) {
            if (!sender.hasPermission("desertjungle.team.admin")) return noPermission(sender);
            Player target = Bukkit.getPlayer(args[1]); EventTeam team = EventTeam.parse(args[2]);
            if (target == null || team == null) { sender.sendMessage("/team set <player> <desert|jungle>"); return true; }
            teams.setTeam(target, team); return true;
        }
        if (args[0].equalsIgnoreCase("leave")) { teams.leave(player); player.sendMessage("You left your event team."); return true; }
        EventTeam team = EventTeam.parse(args[0]);
        if (team == null) { player.sendMessage("Choose desert or jungle."); return true; }
        if (!teams.join(player, team)) player.sendMessage("That team is full.");
        return true;
    }

    private boolean spawnCommand(Player player, String[] args) {
        if (!player.hasPermission("desertjungle.team.spawn")) return noPermission(player);
        if (args.length != 4 || !args[1].equalsIgnoreCase("set")) { player.sendMessage("/team spawn set <desert|jungle> <1-50>"); return true; }
        EventTeam team = EventTeam.parse(args[2]);
        try {
            int slot = Integer.parseInt(args[3]);
            if (team == null) throw new IllegalArgumentException();
            spawns.set(team, slot, player.getLocation());
            player.sendMessage("Saved " + team.display() + " spawn slot " + slot + "/50 (" + spawns.count(team) + " configured).");
        } catch (IllegalArgumentException exception) { player.sendMessage("Spawn slot must be 1-50."); }
        return true;
    }

    private boolean noPermission(CommandSender sender) { sender.sendMessage("No permission."); return true; }

    @Override public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) return List.of("desert", "jungle", "leave", "set", "spawn");
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) return List.of("set");
        if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("spawn"))) return List.of("desert", "jungle");
        return List.of();
    }
}
