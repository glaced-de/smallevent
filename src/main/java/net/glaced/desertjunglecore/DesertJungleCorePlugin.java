package net.glaced.desertjunglecore;

import net.glaced.desertjunglecore.command.*;
import net.glaced.desertjunglecore.config.PluginConfiguration;
import net.glaced.desertjunglecore.data.SqliteRepository;
import net.glaced.desertjunglecore.event.EventService;
import net.glaced.desertjunglecore.listener.GameListener;
import net.glaced.desertjunglecore.service.GroupService;
import net.glaced.desertjunglecore.service.IconService;
import net.glaced.desertjunglecore.service.MuteService;
import net.glaced.desertjunglecore.service.SpawnService;
import net.glaced.desertjunglecore.service.TeamService;
import net.glaced.desertjunglecore.ui.HudService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DesertJungleCorePlugin extends JavaPlugin {

    private SqliteRepository repository;
    private TeamService teams;
    private SpawnService spawns;
    private GroupService groups;
    private EventService event;
    private HudService hud;
    private MuteService muteService;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        saveResource("groups.yml", false);

        var config = new PluginConfiguration(this);

        // SQLite
        repository = new SqliteRepository(this, config.databaseFile());
        repository.initialize(); // players + muted_players

        // Services
        spawns = new SpawnService(this);
        teams = new TeamService(this, repository, config, spawns);
        groups = new GroupService(this, repository);
        event = new EventService(this, teams, config);
        hud = new HudService(this, teams, event, groups, config);
        muteService = new MuteService(repository);

        // Load data
        teams.loadOnlinePlayers();
        groups.loadDefinitions();
        hud.start();

        // HUD init for online players
        getServer().getOnlinePlayers().forEach(player ->
                player.getScheduler().run(this, ignored -> hud.initialize(player), null)
        );

        // Listener (jetzt mit MuteService!)
        getServer().getPluginManager().registerEvents(
                new GameListener(this, teams, event, hud, muteService),
                this
        );

        // Commands
        register("team", new TeamCommand(teams, spawns));
        register("event", new EventCommand(event));
        register("group", new GroupCommand(groups));
        register("speed", new SpeedCommand(this));
        register("vanish", new VanishCommand(this));
        register("gm", new GamemodeCommand(this));
        register("mute", new MuteCommand(this, muteService));

        getLogger().info("DesertJungleCore enabled.");
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null)
            throw new IllegalStateException("Missing command in plugin.yml: " + name);

        command.setExecutor(executor);

        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public MuteService getMuteService() {
        return muteService;
    }

    @Override
    public void onDisable() {
        if (hud != null) hud.stop();
        if (repository != null) repository.close();
    }
}
