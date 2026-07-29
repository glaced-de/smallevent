package net.glaced.desertjunglecore.data;

import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;

import net.glaced.desertjunglecore.api.EventTeam;
import org.bukkit.plugin.java.JavaPlugin;

/** SQLite DAO. All calls use a dedicated executor; Bukkit objects never enter this layer. */
public final class SqliteRepository implements AutoCloseable {

    private final JavaPlugin plugin;
    private final File file;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "djc-sqlite"));

    public SqliteRepository(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    public void initialize() {

        // EXISTING TABLE (unchanged, first_assigned_at added via migration below)
        join(run("""
            CREATE TABLE IF NOT EXISTS players (
                uuid TEXT PRIMARY KEY,
                team TEXT,
                group_name TEXT NOT NULL DEFAULT 'default',
                kills INTEGER NOT NULL DEFAULT 0,
                deaths INTEGER NOT NULL DEFAULT 0,
                assists INTEGER NOT NULL DEFAULT 0,
                first_assigned_at INTEGER NOT NULL DEFAULT 0
            )
        """));

        // Migration for existing databases created before first_assigned_at existed.
        join(addColumnIfMissing("players", "first_assigned_at", "INTEGER NOT NULL DEFAULT 0"));

        // NEW TABLE (added, does not break anything)
        join(run("""
            CREATE TABLE IF NOT EXISTS muted_players (
                uuid TEXT PRIMARY KEY
            )
        """));
    }

    /** Adds a column if it doesn't already exist. SQLite has no "ADD COLUMN IF NOT EXISTS", so we check pragma first. */
    private CompletableFuture<Void> addColumnIfMissing(String table, String column, String definition) {
        return CompletableFuture.runAsync(() -> {
            try (var c = connection()) {
                boolean exists = false;
                try (var s = c.prepareStatement("PRAGMA table_info(" + table + ")"); var r = s.executeQuery()) {
                    while (r.next()) {
                        if (r.getString("name").equalsIgnoreCase(column)) { exists = true; break; }
                    }
                }
                if (!exists) {
                    try (var s = c.createStatement()) {
                        s.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                    }
                }
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, io);
    }

    /** Public so MuteService can use it */
    public Connection connection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    /** Public so MuteService can use it */
    public CompletableFuture<Void> run(String sql) {
        return CompletableFuture.runAsync(() -> {
            try (var c = connection(); var s = c.createStatement()) {
                s.execute(sql);
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, io);
    }

    public CompletableFuture<PlayerData> load(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try (var c = connection();
                 var s = c.prepareStatement("SELECT * FROM players WHERE uuid=?")) {

                s.setString(1, id.toString());
                var r = s.executeQuery();

                return r.next()
                        ? new PlayerData(id, EventTeam.parse(r.getString("team")),
                        r.getString("group_name"),
                        r.getInt("kills"),
                        r.getInt("deaths"),
                        r.getInt("assists"),
                        r.getLong("first_assigned_at"))
                        : PlayerData.empty(id);

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, io);
    }

    public CompletableFuture<Void> save(PlayerData d) {
        return CompletableFuture.runAsync(() -> {
            try (var c = connection();
                 var s = c.prepareStatement("""
                    INSERT INTO players(uuid,team,group_name,kills,deaths,assists,first_assigned_at)
                    VALUES(?,?,?,?,?,?,?)
                    ON CONFLICT(uuid) DO UPDATE SET
                        team=excluded.team,
                        group_name=excluded.group_name,
                        kills=excluded.kills,
                        deaths=excluded.deaths,
                        assists=excluded.assists,
                        first_assigned_at=excluded.first_assigned_at
                """)) {

                s.setString(1, d.id().toString());
                s.setString(2, d.team() == null ? null : d.team().name());
                s.setString(3, d.group());
                s.setInt(4, d.kills());
                s.setInt(5, d.deaths());
                s.setInt(6, d.assists());
                s.setLong(7, d.firstAssignedAt());
                s.executeUpdate();

            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, io);
    }

    private void join(CompletableFuture<?> future) {
        try {
            future.join();
        } catch (CompletionException e) {
            plugin.getLogger().severe("Database initialization failed: " + e.getCause().getMessage());
            throw e;
        }
    }

    @Override
    public void close() {
        io.shutdown();
    }
}