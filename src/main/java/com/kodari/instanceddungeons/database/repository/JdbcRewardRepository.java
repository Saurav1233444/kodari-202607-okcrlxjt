package com.kodari.instanceddungeons.database.repository;

import com.kodari.instanceddungeons.domain.RewardRecord;
import com.kodari.instanceddungeons.repositories.RewardRepository;

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

public final class JdbcRewardRepository implements RewardRepository {
    private final DataSource dataSource;
    private final Executor executor;

    public JdbcRewardRepository(DataSource dataSource, Executor executor) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    public CompletableFuture<List<RewardRecord>> findByPlayer(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT id, instance_id, player_id, reward_type, payload, created_at FROM rewards WHERE player_id = ?";
            List<RewardRecord> list = new ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, playerId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new RewardRecord(
                                UUID.fromString(rs.getString("id")),
                                UUID.fromString(rs.getString("instance_id")),
                                UUID.fromString(rs.getString("player_id")),
                                rs.getString("reward_type"),
                                rs.getString("payload"),
                                rs.getLong("created_at")
                        ));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to find rewards by player", e);
            }
            return Collections.unmodifiableList(list);
        }, executor);
    }

    @Override
    public CompletableFuture<Void> save(RewardRecord reward) {
        return CompletableFuture.runAsync(() -> {
            String sql = "INSERT INTO rewards (id, instance_id, player_id, reward_type, payload, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, reward.id().toString());
                stmt.setString(2, reward.instanceId().toString());
                stmt.setString(3, reward.playerId().toString());
                stmt.setString(4, reward.type());
                stmt.setString(5, reward.payload());
                stmt.setLong(6, reward.createdAt());
                stmt.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to save reward record", e);
            }
        }, executor);
    }
}
