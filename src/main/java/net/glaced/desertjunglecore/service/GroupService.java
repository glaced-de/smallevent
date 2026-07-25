package net.glaced.desertjunglecore.service;

import java.io.File; import java.util.*; import java.util.concurrent.ConcurrentHashMap;
import net.glaced.desertjunglecore.data.*; import net.kyori.adventure.text.minimessage.MiniMessage; import org.bukkit.configuration.ConfigurationSection; import org.bukkit.configuration.file.YamlConfiguration; import org.bukkit.entity.Player; import org.bukkit.plugin.java.JavaPlugin;

/** Local permission-group service. Definitions are in groups.yml; assignments are persisted in SQLite. */
public final class GroupService {
    private final JavaPlugin plugin; private final SqliteRepository repository; private final Map<String,Group> groups=new ConcurrentHashMap<>(); private final Map<UUID,String> assigned=new ConcurrentHashMap<>();
    public GroupService(JavaPlugin plugin, SqliteRepository repository){this.plugin=plugin;this.repository=repository;}
    public void loadDefinitions(){var y=YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(),"groups.yml"));var root=y.getConfigurationSection("groups");if(root==null)return;for(String n:root.getKeys(false)){var s=root.getConfigurationSection(n);groups.put(n,new Group(n,s.getInt("weight"),s.getString("prefix",""),s.getString("suffix",""),s.getStringList("inherits"),s.getStringList("permissions")));}}
    public Group get(String name){return groups.getOrDefault(name,groups.get("default"));}
    public String prefix(Player p){return get(assigned.getOrDefault(p.getUniqueId(),"default")).prefix();}
    public void assign(Player target,String group){if(!groups.containsKey(group))throw new IllegalArgumentException("Unknown group");assigned.put(target.getUniqueId(),group);repository.load(target.getUniqueId()).thenAccept(d->repository.save(new PlayerData(d.id(),d.team(),group,d.kills(),d.deaths(),d.assists())));}
    public record Group(String name,int weight,String prefix,String suffix,List<String> inherits,List<String> permissions){}
}
