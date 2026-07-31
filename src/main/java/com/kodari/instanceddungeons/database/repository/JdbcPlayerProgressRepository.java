package com.kodari.instanceddungeons.database.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kodari.instanceddungeons.domain.PlayerProgress;
import com.kodari.instanceddungeons.repositories.PlayerProgressRepository;

import javax.sql.DataSource;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class JdbcPlayerProgressRepository implements PlayerProgressRepository {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcPlayerProgressRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<PlayerProgress> find(UUID playerId, UUID instanceId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT player_id, instance_id, state, serialized_data, updated_at FROM player_progress WHERE player_id = ? AND instance_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, instanceId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> data = GSON.fromJson(rs.getString("serialized_data"), MAP_TYPE);
                        if (data == null) data = Collections.emptyMap();
                        return new PlayerProgress(
                                UUID.fromString(rs.getString("player_id")),
                                UUID.fromString(rs.getString("instance_id")),
                                rs.getString("state"),
                                data,
                                rs.getLong("updated_at")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find player progress", e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(PlayerProgress progress) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO player_progress (player_id, instance_id, state, serialized_data, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(player_id, instance_id) DO UPDATE SET
                    state = EXCLUDED.state,
                    serialized_data = EXCLUDED.serialized_data,
                    updated_at = EXCLUDED.updated_at
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, progress.playerId().toString());
                stmt.setString(2, progress.instanceId().toString());
                stmt.setString(3, progress.state());
                stmt.setString(4, GSON.toJson(progress.data()));
                stmt.setLong(5, progress.updatedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                saveFallback(progress);
            }
        }, executor);
    }

    private void saveFallback(PlayerProgress progress) {
        String sql = """
            INSERT INTO player_progress (player_id, instance_id, state, serialized_data, updated_at)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                state = VALUES(state),
                serialized_data = VALUES(serialized_data),
                updated_at = VALUES(updated_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, progress.playerId().toString());
            stmt.setString(2, progress.instanceId().toString());
            stmt.setString(3, progress.state());
            stmt.setString(4, GSON.toJson(progress.data()));
            stmt.setLong(5, progress.updatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save player progress", e);
        }
    }

    @Override
    public CompletableFuture<Void> delete(UUID playerId, UUID instanceId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_progress WHERE player_id = ? AND instance_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, instanceId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete player progress", e);
            }
        }, executor);
    }
}
