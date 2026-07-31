package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.repositories.CooldownRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class JdbcCooldownRepository implements CooldownRepository {
    private final DataSource dataSource;
    private final Executor executor;

    public JdbcCooldownRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<Long> findExpiry(UUID playerId, String key) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT expires_at FROM cooldowns WHERE player_id = ? AND cooldown_key = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("expires_at");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find cooldown expiry", e);
            }
            return 0L;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(UUID playerId, String key, long expiresAt) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO cooldowns (player_id, cooldown_key, expires_at)
                VALUES (?, ?, ?)
                ON CONFLICT(player_id, cooldown_key) DO UPDATE SET
                    expires_at = EXCLUDED.expires_at
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                stmt.setLong(3, expiresAt);
                stmt.executeUpdate();
            } catch (SQLException e) {
                saveFallback(playerId, key, expiresAt);
            }
        }, executor);
    }

    private void saveFallback(UUID playerId, String key, long expiresAt) {
        String sql = """
            INSERT INTO cooldowns (player_id, cooldown_key, expires_at)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                expires_at = VALUES(expires_at)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, key);
            stmt.setLong(3, expiresAt);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save cooldown", e);
        }
    }

    @Override
    public CompletableFuture<Void> delete(UUID playerId, String key) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM cooldowns WHERE player_id = ? AND cooldown_key = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to delete cooldown", e);
            }
        }, executor);
    }
}
