package net.glaced.desertjunglecore.data;
import java.util.UUID;
import net.glaced.desertjunglecore.api.EventTeam;
public record PlayerData(UUID id, EventTeam team, String group, int kills, int deaths, int assists) {
    public static PlayerData empty(UUID id) { return new PlayerData(id, null, "default", 0, 0, 0); }
}
