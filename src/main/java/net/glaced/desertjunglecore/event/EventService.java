package net.glaced.desertjunglecore.event;

import net.glaced.desertjunglecore.api.*; import net.glaced.desertjunglecore.config.PluginConfiguration; import net.glaced.desertjunglecore.service.TeamService;
import net.kyori.adventure.bossbar.BossBar; import net.kyori.adventure.text.Component; import net.kyori.adventure.text.format.NamedTextColor; import org.bukkit.Bukkit; import org.bukkit.Sound; import org.bukkit.plugin.java.JavaPlugin; import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/** Event lifecycle and the shared countdown BossBar. */
public final class EventService {
    private final JavaPlugin plugin; private final TeamService teams; private final PluginConfiguration config; private volatile EventState state=EventState.IDLE; private ScheduledTask countdown; private BossBar bar;
    public EventService(JavaPlugin plugin,TeamService teams,PluginConfiguration config){this.plugin=plugin;this.teams=teams;this.config=config;}
    public EventState state(){return state;}
    public void start(){if(state!=EventState.IDLE&&state!=EventState.FINISHED)return;state=EventState.COUNTDOWN;int total=config.countdownSeconds();bar=BossBar.bossBar(Component.text("Event starts in " + total + " seconds",NamedTextColor.GOLD),1f,BossBar.Color.YELLOW,BossBar.Overlay.PROGRESS);Bukkit.getOnlinePlayers().forEach(p->p.getScheduler().run(plugin,t->p.showBossBar(bar),null));final int[] remaining={total};countdown=Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin,ignored->{int s=remaining[0]--;bar.name(Component.text("Event starts in " + s + " seconds",NamedTextColor.GOLD));bar.progress(Math.max(0f,(float)s/total));if(s<=0){countdown.cancel();teams.teleportAllToSpawns();Bukkit.getOnlinePlayers().forEach(p->p.getScheduler().run(plugin,t->{p.hideBossBar(bar);p.playSound(p,Sound.BLOCK_BEACON_ACTIVATE,1,1);p.sendMessage(Component.text("Event is now running.",NamedTextColor.GREEN));},null));state=EventState.RUNNING;}},20,20);}
    public void stop(){if(countdown!=null)countdown.cancel();if(bar!=null)Bukkit.getOnlinePlayers().forEach(p->p.hideBossBar(bar));state=EventState.FINISHED;}
    public void pause(){if(state==EventState.RUNNING)state=EventState.PAUSED;} public void resume(){if(state==EventState.PAUSED)state=EventState.RUNNING;}
}
