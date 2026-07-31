package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.domain.InstanceRecord;
import com.kodari.instanceddungeons.repositories.InstanceRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class JdbcInstanceRepository implements InstanceRepository {
    private final DataSource dataSource;
    private final Executor executor;

    public JdbcInstanceRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<InstanceRecord> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, dungeon_id, world_name, state, created_at, completed_at FROM active_instances WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        long completed = rs.getLong("completed_at");
                        Long completedAt = rs.wasNull() ? null : completed;
                        return new InstanceRecord(
                                UUID.fromString(rs.getString("id")),
                                UUID.fromString(rs.getString("dungeon_id")),
                                rs.getString("world_name"),
                                rs.getString("state"),
                                rs.getLong("created_at"),
                                completedAt
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find instance record by id", e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<List<InstanceRecord>> findActive() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, dungeon_id, world_name, state, created_at, completed_at FROM active_instances WHERE state != 'COMPLETED' AND state != 'FAILED'";
            List<InstanceRecord> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long completed = rs.getLong("completed_at");
                    Long completedAt = rs.wasNull() ? null : completed;
                    list.add(new InstanceRecord(
                            UUID.fromString(rs.getString("id")),
                            UUID.fromString(rs.getString("dungeon_id")),
                            rs.getString("world_name"),
                            rs.getString("state"),
                            rs.getLong("created_at"),
                            completedAt
                    ));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find active instances", e);
            }
            return Collections.unmodifiableList(list);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(InstanceRecord instance) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO active_instances (id, dungeon_id, world_name, state, created_at, completed_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    dungeon_id = EXCLUDED.dungeon_id,
                    world_name = EXCLUDED.world_name,
                    state = EXCLUDED.state,
                    created_at = EXCLUDED.created_at,
                    completed_at = EXCLUDED.completed_at
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, instance.id().toString());
                stmt.setString(2, instance.dungeonId().toString());
                stmt.setString(3, instance.worldName());
                stmt.setString(4, instance.state());
                stmt.setLong(5, instance.createdAt());
                if (instance.completedAt() != null) {
                    stmt.setLong(6, instance.completedAt());
                } else {
                    stmt.setNull(6, java.sql.Types.BIGINT);
                }
                stmt.executeUpdate();
            } catch (SQLException e) {
                saveFallback(instance);
            }
        }, executor);
    }

    private void saveFallback(InstanceRecord instance) {
        String sql = """
            INSERT INTO active_instances (id, dungeon_id, world_name, state, created_at, completed_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                dungeon_id = VALUES(dungeon_id),
                world_name = VALUES(world_name),
                state = VALUES(state),
                created_at = VALUES(created_at),
                completed_at = VALUES(completed_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, instance.id().toString());
            stmt.setString(2, instance.dungeonId().toString());
            stmt.setString(3, instance.worldName());
            stmt.setString(4, instance.state());
            stmt.setLong(5, instance.createdAt());
            if (instance.completedAt() != null) {
                stmt.setLong(6, instance.completedAt());
            } else {
                stmt.setNull(6, java.sql.Types.BIGINT);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save instance record", e);
        }
    }

    @Override
    public CompletableFuture<Void> delete(UUID id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM active_instances WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete instance record", e);
            }
        }, executor);
    }
}
