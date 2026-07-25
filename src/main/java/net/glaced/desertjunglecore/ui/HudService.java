package net.glaced.desertjunglecore.ui;

import fr.mrmicky.fastboard.adventure.FastBoard;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.glaced.desertjunglecore.api.EventState;
import net.glaced.desertjunglecore.api.EventTeam;
import net.glaced.desertjunglecore.config.PluginConfiguration;
import net.glaced.desertjunglecore.event.EventService;
import net.glaced.desertjunglecore.service.GroupService;
import net.glaced.desertjunglecore.service.TeamService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class HudService {

    private static final TextColor DESERT = TextColor.color(0xF2C572);
    private static final TextColor JUNGLE = TextColor.color(0x4CD07D);
    private static final TextColor DISCORD_BLUE = TextColor.color(0x5865F2);
    private static final TextColor LABEL_GRAY = TextColor.color(0x8A8A8A);
    private static final TextColor ARROW_GRAY = TextColor.color(0x6B6B6B);

    private static final Component BLANK = Component.empty();
    private static final Component TITLE = gradient("GLACED EVENTS", DESERT, JUNGLE, true);
    private static final Component HEADER = TITLE;
    private static final Component FOOTER = Component.text()
            .append(Component.text("Discord", DISCORD_BLUE))
            .append(arrow())
            .append(Component.text("discord.gg/p3C625MsvE", NamedTextColor.WHITE))
            .build();

    private static final Component TEAM_VS = Component.text("  vs  ", ARROW_GRAY);

    private static final Component ONLINE_LABEL = Component.text("Online", LABEL_GRAY).append(arrow());
    private static final Component KILLS_LABEL = Component.text("Kills", LABEL_GRAY).append(arrow());
    private static final Component DEATHS_LABEL = Component.text("Deaths", LABEL_GRAY).append(arrow());
    private static final Component KD_LABEL = Component.text("K/D", LABEL_GRAY).append(arrow());
    private static final Component STATUS_LABEL = Component.text("Status", LABEL_GRAY).append(arrow());
    private static final Component SPACER = Component.text("   ");

    private static final Map<EventState, Component> STATE_BADGES = buildStateBadges();

    private static final Component ACTIONBAR_DESERT_LABEL = Component.text("Desert ", DESERT);
    private static final Component ACTIONBAR_JUNGLE_LABEL = Component.text(" | Jungle ", JUNGLE);
    private static final Component ACTIONBAR_HP_LABEL = Component.text(" | HP ", NamedTextColor.RED);

    private final JavaPlugin plugin;
    private final TeamService teams;
    private final EventService event;

    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();
    private final Map<UUID, SidebarState> lastSidebar = new ConcurrentHashMap<>();
    private final Map<UUID, ActionBarState> lastActionBar = new ConcurrentHashMap<>();

    private ScheduledTask actionBarTask;
    private ScheduledTask sidebarTask;

    public HudService(JavaPlugin plugin, TeamService teams, EventService event, GroupService ignoredGroups, PluginConfiguration ignoredConfig) {
        this.plugin = plugin;
        this.teams = teams;
        this.event = event;
    }

    public void start() {
        actionBarTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> dispatch(this::updateActionBar),
                1L,
                20L
        );

        sidebarTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                plugin,
                task -> dispatch(this::updateSidebar),
                1L,
                40L
        );
    }

    public void stop() {
        if (actionBarTask != null) actionBarTask.cancel();
        if (sidebarTask != null) sidebarTask.cancel();
        boards.values().forEach(FastBoard::delete);
        boards.clear();
        lastSidebar.clear();
        lastActionBar.clear();
    }

    public void initialize(Player player) {
        boards.computeIfAbsent(player.getUniqueId(), ignored -> {
            FastBoard board = new FastBoard(player);
            board.updateTitle(TITLE);
            return board;
        });
        player.sendPlayerListHeaderAndFooter(HEADER, FOOTER);
    }

    public void remove(Player player) {
        UUID id = player.getUniqueId();
        FastBoard board = boards.remove(id);
        if (board != null) board.delete();
        lastSidebar.remove(id);
        lastActionBar.remove(id);
    }

    private void dispatch(Consumer<Player> update) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getScheduler().run(
                    plugin,
                    task -> update.accept(player),
                    null
            );
        }
    }

    private void updateActionBar(Player player) {
        UUID id = player.getUniqueId();
        int desert = teams.size(EventTeam.DESERT);
        int jungle = teams.size(EventTeam.JUNGLE);
        int hp = (int) Math.ceil(player.getHealth());

        ActionBarState state = new ActionBarState(desert, jungle, hp);
        if (state.equals(lastActionBar.put(id, state))) return;

        player.sendActionBar(Component.text()
                .append(ACTIONBAR_DESERT_LABEL)
                .append(Component.text(desert, NamedTextColor.WHITE))
                .append(ACTIONBAR_JUNGLE_LABEL)
                .append(Component.text(jungle, NamedTextColor.WHITE))
                .append(ACTIONBAR_HP_LABEL)
                .append(Component.text(hp, NamedTextColor.WHITE))
                .build());
    }

    private void updateSidebar(Player player) {
        UUID id = player.getUniqueId();
        FastBoard board = boards.get(id);
        if (board == null) return;

        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        int desert = teams.size(EventTeam.DESERT);
        int jungle = teams.size(EventTeam.JUNGLE);
        int kills = teams.kills(id);
        int deaths = teams.deaths(id);
        EventState eventState = event.state();

        SidebarState state = new SidebarState(online, max, desert, jungle, kills, deaths, eventState);
        if (state.equals(lastSidebar.put(id, state))) return;

        String kd = String.format(Locale.ROOT, "%.2f", deaths == 0 ? kills : (double) kills / deaths);

        List<Component> lines = new ArrayList<>(9);
        lines.add(Component.text()
                .append(Component.text("Desert", DESERT, TextDecoration.BOLD))
                .append(Component.text(" " + desert, NamedTextColor.WHITE))
                .append(TEAM_VS)
                .append(Component.text("Jungle", JUNGLE, TextDecoration.BOLD))
                .append(Component.text(" " + jungle, NamedTextColor.WHITE))
                .build());
        lines.add(BLANK);
        lines.add(Component.text().append(ONLINE_LABEL).append(Component.text(online + "/" + max, NamedTextColor.YELLOW)).build());
        lines.add(BLANK);
        lines.add(Component.text()
                .append(KILLS_LABEL).append(Component.text(kills, NamedTextColor.WHITE))
                .append(SPACER)
                .append(DEATHS_LABEL).append(Component.text(deaths, NamedTextColor.WHITE))
                .build());
        lines.add(Component.text().append(KD_LABEL).append(Component.text(kd, NamedTextColor.YELLOW)).build());
        lines.add(BLANK);
        lines.add(Component.text().append(STATUS_LABEL).append(STATE_BADGES.get(eventState)).build());

        board.updateLines(lines);
    }

    private static Component gradient(String text, TextColor from, TextColor to, boolean bold) {
        int length = text.length();
        TextComponent.Builder builder = Component.text();
        for (int i = 0; i < length; i++) {
            float ratio = length == 1 ? 0f : (float) i / (length - 1);
            TextColor color = TextColor.lerp(ratio, from, to);
            Style style = bold ? Style.style(color, TextDecoration.BOLD) : Style.style(color);
            builder.append(Component.text(String.valueOf(text.charAt(i)), style));
        }
        return builder.build();
    }

    private static Component arrow() {
        return Component.text(" \u00BB ", ARROW_GRAY);
    }

    private static Map<EventState, Component> buildStateBadges() {
        Map<EventState, Component> map = new EnumMap<>(EventState.class);
        map.put(EventState.RUNNING, Component.text("LIVE", NamedTextColor.GREEN, TextDecoration.BOLD));
        map.put(EventState.COUNTDOWN, Component.text("STARTING", NamedTextColor.YELLOW, TextDecoration.BOLD));
        map.put(EventState.PAUSED, Component.text("PAUSED", NamedTextColor.GRAY, TextDecoration.BOLD));
        map.put(EventState.FINISHED, Component.text("ENDED", NamedTextColor.RED, TextDecoration.BOLD));
        map.put(EventState.IDLE, Component.text("WAITING", NamedTextColor.GRAY, TextDecoration.BOLD));
        return map;
    }

    private record SidebarState(int online, int max, int desert, int jungle, int kills, int deaths, EventState state) {}

    private record ActionBarState(int desert, int jungle, int hp) {}
}
