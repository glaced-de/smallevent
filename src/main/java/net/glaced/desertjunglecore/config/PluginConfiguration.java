package net.glaced.desertjunglecore.config;

import java.io.File;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/** Typed access to the user-editable plugin configuration. */
public record PluginConfiguration(JavaPlugin plugin) {
    private FileConfiguration c() { return plugin.getConfig(); }
    public File databaseFile() { return new File(plugin.getDataFolder(), c().getString("database.file", "database.db")); }
    public int maxTeamSize() { return c().getInt("teams.maximum-size", 50); }
    public boolean friendlyFire() { return c().getBoolean("teams.friendly-fire", false); }
    public boolean autoBalance() { return c().getBoolean("teams.auto-balance", true); }
    public int countdownSeconds() { return c().getInt("event.countdown-seconds", 600); }
    public int respawnSeconds() { return c().getInt("event.respawn-seconds", 5); }
    public String text(String key) { return c().getString(key, "<red>Missing config: " + key); }
}
