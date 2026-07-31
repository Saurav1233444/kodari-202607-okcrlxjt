package com.kodari.instanceddungeons.database.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kodari.instanceddungeons.domain.DungeonDefinition;
import com.kodari.instanceddungeons.repositories.DungeonDefinitionRepository;

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

public final class JdbcDungeonDefinitionRepository implements DungeonDefinitionRepository {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final DataSource dataSource;
    private final Executor executor;

    public JdbcDungeonDefinitionRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<DungeonDefinition> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, dungeon_key, display_name, template_world, serialized_data, updated_at FROM dungeon_definitions WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> data = GSON.fromJson(rs.getString("serialized_data"), MAP_TYPE);
                        if (data == null) data = Collections.emptyMap();
                        return new DungeonDefinition(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("dungeon_key"),
                                rs.getString("display_name"),
                                rs.getString("template_world"),
                                data,
                                rs.getLong("updated_at")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find dungeon definition by id", e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<DungeonDefinition> findByKey(String key) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, dungeon_key, display_name, template_world, serialized_data, updated_at FROM dungeon_definitions WHERE dungeon_key = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> data = GSON.fromJson(rs.getString("serialized_data"), MAP_TYPE);
                        if (data == null) data = Collections.emptyMap();
                        return new DungeonDefinition(
                                UUID.fromString(rs.getString("id")),
                                rs.getString("dungeon_key"),
                                rs.getString("display_name"),
                                rs.getString("template_world"),
                                data,
                                rs.getLong("updated_at")
                        );
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find dungeon definition by key", e);
            }
            return null;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(DungeonDefinition definition) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO dungeon_definitions (id, dungeon_key, display_name, template_world, serialized_data, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(id) DO UPDATE SET
                    dungeon_key = EXCLUDED.dungeon_key,
                    display_name = EXCLUDED.display_name,
                    template_world = EXCLUDED.template_world,
                    serialized_data = EXCLUDED.serialized_data,
                    updated_at = EXCLUDED.updated_at
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, definition.id().toString());
                stmt.setString(2, definition.key());
                stmt.setString(3, definition.displayName());
                stmt.setString(4, definition.templateWorld());
                stmt.setString(5, GSON.toJson(definition.data()));
                stmt.setLong(6, definition.updatedAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                saveFallback(definition);
            }
        }, executor);
    }

    private void saveFallback(DungeonDefinition definition) {
        String sql = """
            INSERT INTO dungeon_definitions (id, dungeon_key, display_name, template_world, serialized_data, updated_at)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                dungeon_key = VALUES(dungeon_key),
                display_name = VALUES(display_name),
                template_world = VALUES(template_world),
                serialized_data = VALUES(serialized_data),
                updated_at = VALUES(updated_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, definition.id().toString());
            stmt.setString(2, definition.key());
            stmt.setString(3, definition.displayName());
            stmt.setString(4, definition.templateWorld());
            stmt.setString(5, GSON.toJson(definition.data()));
            stmt.setLong(6, definition.updatedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save dungeon definition", e);
        }
    }

    @Override
    public CompletableFuture<Void> delete(UUID id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM dungeon_definitions WHERE id = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete dungeon definition", e);
            }
        }, executor);
    }
}
