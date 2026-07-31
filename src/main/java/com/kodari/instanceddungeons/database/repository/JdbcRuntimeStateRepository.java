package com.kodari.instanceddungeons.database.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kodari.instanceddungeons.domain.RuntimeState;
import com.kodari.instanceddungeons.repositories.RuntimeStateRepository;

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

public final class JdbcRuntimeStateRepository implements RuntimeStateRepository {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcRuntimeStateRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<RuntimeState> find(UUID instanceId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT instance_id, state, serialized_data, updated_at FROM runtime_state WHERE instance_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, instanceId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> data = GSON.fromJson(rs.getString("serialized_data"), MAP_TYPE);
                        if (data == null) data = Collections.emptyMap();
                        return new RuntimeState(
                                UUID.fromString(rs.getString("instance_id")),
                                rs.getString("state"),
                                data,
                                rs.getLong("updated_at")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find runtime state", e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(RuntimeState state) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO runtime_state (instance_id, state, serialized_data, updated_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(instance_id) DO UPDATE SET
                    state = EXCLUDED.state,
                    serialized_data = EXCLUDED.serialized_data,
                    updated_at = EXCLUDED.updated_at
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, state.instanceId().toString());
                stmt.setString(2, state.state());
                stmt.setString(3, GSON.toJson(state.data()));
                stmt.setLong(4, state.updatedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                saveFallback(state);
            }
        }, executor);
    }

    private void saveFallback(RuntimeState state) {
        String sql = """
            INSERT INTO runtime_state (instance_id, state, serialized_data, updated_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                state = VALUES(state),
                serialized_data = VALUES(serialized_data),
                updated_at = VALUES(updated_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, state.instanceId().toString());
            stmt.setString(2, state.state());
            stmt.setString(3, GSON.toJson(state.data()));
            stmt.setLong(4, state.updatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save runtime state", e);
        }
    }

    @Override
    public CompletableFuture<Void> delete(UUID instanceId) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM runtime_state WHERE instance_id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, instanceId.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete runtime state", e);
            }
        }, executor);
    }
}
