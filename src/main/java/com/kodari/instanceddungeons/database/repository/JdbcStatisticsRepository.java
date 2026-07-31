package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.repositories.StatisticsRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class JdbcStatisticsRepository implements StatisticsRepository {
    private final DataSource dataSource;
    private final Executor executor;

    public JdbcStatisticsRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<Long> get(UUID playerId, String key) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT statistic_value FROM statistics WHERE player_id = ? AND statistic_key = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("statistic_value");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to get statistic", e);
            }
            return 0L;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> set(UUID playerId, String key, long value) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO statistics (player_id, statistic_key, statistic_value)
                VALUES (?, ?, ?)
                ON CONFLICT(player_id, statistic_key) DO UPDATE SET
                    statistic_value = EXCLUDED.statistic_value
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                stmt.setLong(3, value);
                stmt.executeUpdate();
            } catch (SQLException e) {
                setFallback(playerId, key, value);
            }
        }, executor);
    }

    private void setFallback(UUID playerId, String key, long value) {
        String sql = """
            INSERT INTO statistics (player_id, statistic_key, statistic_value)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                statistic_value = VALUES(statistic_value)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, key);
            stmt.setLong(3, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to set statistic", e);
        }
    }

    @Override
    public CompletableFuture<Void> increment(UUID playerId, String key, long amount) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                INSERT INTO statistics (player_id, statistic_key, statistic_value)
                VALUES (?, ?, ?)
                ON CONFLICT(player_id, statistic_key) DO UPDATE SET
                    statistic_value = statistics.statistic_value + EXCLUDED.statistic_value
                """;
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                stmt.setString(2, key);
                stmt.setLong(3, amount);
                stmt.executeUpdate();
            } catch (SQLException e) {
                incrementFallback(playerId, key, amount);
            }
        }, executor);
    }

    private void incrementFallback(UUID playerId, String key, long amount) {
        String sql = """
            INSERT INTO statistics (player_id, statistic_key, statistic_value)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE
                statistic_value = statistic_value + VALUES(statistic_value)
            """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerId.toString());
            stmt.setString(2, key);
            stmt.setLong(3, amount);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to increment statistic", e);
        }
    }
}
