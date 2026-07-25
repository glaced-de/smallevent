package net.glaced.desertjunglecore.service;

import net.glaced.desertjunglecore.data.SqliteRepository;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletionException;

public class MuteService {

    private final SqliteRepository repo;
    private final Set<UUID> muted = new HashSet<>();

    public MuteService(SqliteRepository repo) {
        this.repo = repo;
        loadMuted();
    }

    private void loadMuted() {
        try (var c = repo.connection();
             var s = c.prepareStatement("SELECT uuid FROM muted_players");
             var rs = s.executeQuery()) {

            while (rs.next()) {
                muted.add(UUID.fromString(rs.getString("uuid")));
            }

        } catch (SQLException e) {
            throw new CompletionException(e);
        }
    }

    public boolean isMuted(UUID id) {
        return muted.contains(id);
    }

    public void mute(UUID id) {
        muted.add(id);
        repo.run("INSERT OR IGNORE INTO muted_players (uuid) VALUES ('" + id + "')");
    }

    public void unmute(UUID id) {
        muted.remove(id);
        repo.run("DELETE FROM muted_players WHERE uuid = '" + id + "'");
    }
}
