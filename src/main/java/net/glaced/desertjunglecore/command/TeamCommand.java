package net.glaced.desertjunglecore.command;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
        if (args.length == 0) { player.sendMessage("/team <join|leave|spawn>"); return true; }
        if (args[0].equalsIgnoreCase("spawn")) return spawnCommand(player, args);
        if (args[0].equalsIgnoreCase("set") && args.length == 3) {
            if (!sender.hasPermission("desertjungle.team.admin")) return noPermission(sender);
            Player target = Bukkit.getPlayer(args[1]); EventTeam team = EventTeam.parse(args[2]);
            if (target == null || team == null) { sender.sendMessage("/team set <player> <team1|team2>"); return true; }
            teams.setTeam(target, team);
            sender.sendMessage("Set " + target.getName() + " to " + team.display() + ".");
            return true;
        }
        if (args[0].equalsIgnoreCase("leave")) { teams.leave(player); player.sendMessage("You left your event team."); return true; }
        if (args[0].equalsIgnoreCase("join")) {
            EventTeam assigned = joinRandom(player);
            if (assigned == null) { player.sendMessage("Both teams are full."); return true; }
            player.sendMessage("You have been randomly assigned to " + assigned.display() + ".");
            return true;
        }
        player.sendMessage("/team <join|leave|spawn>");
        return true;
    }

    private EventTeam joinRandom(Player player) {
        EventTeam first = ThreadLocalRandom.current().nextBoolean() ? EventTeam.TEAM1 : EventTeam.TEAM2;
        EventTeam second = first == EventTeam.TEAM1 ? EventTeam.TEAM2 : EventTeam.TEAM1;
        if (teams.join(player, first)) return first;
        if (teams.join(player, second)) return second;
        return null;
    }

    private boolean spawnCommand(Player player, String[] args) {
        if (!player.hasPermission("desertjungle.team.spawn")) return noPermission(player);
        if (args.length != 4 || !args[1].equalsIgnoreCase("set")) { player.sendMessage("/team spawn set <team1|team2> <1-50>"); return true; }
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
        if (args.length == 1) {
            List<String> options = new ArrayList<>(List.of("join", "leave"));
            if (sender.hasPermission("desertjungle.team.admin")) options.add("set");
            if (sender.hasPermission("desertjungle.team.spawn")) options.add("spawn");
            return filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn") && sender.hasPermission("desertjungle.team.spawn")) {
            return filter(List.of("set"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set") && sender.hasPermission("desertjungle.team.admin")) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set") && sender.hasPermission("desertjungle.team.admin")) {
            return filter(List.of("team1", "team2"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("spawn") && sender.hasPermission("desertjungle.team.spawn")) {
            return filter(List.of("team1", "team2"), args[2]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).toList();
    }
}