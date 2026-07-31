package com.kodari.instanceddungeons.database.migration;

import com.kodari.instanceddungeons.logging.LoggingService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

public final class MigrationRunner {
    private static final List<Migration> MIGRATIONS = List.of(
            new Migration(1, "initial persistence schema", MigrationRunner::createInitialSchema)
    );

    private final DataSource dataSource;
    private final LoggingService logging;

    public MigrationRunner(DataSource dataSource, LoggingService logging) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.logging = Objects.requireNonNull(logging, "logging");
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                createHistoryTable(connection);
                int currentVersion = currentVersion(connection);
                for (Migration migration : MIGRATIONS) {
                    if (migration.version() <= currentVersion) {
                        continue;
                    }
                    migration.action().apply(connection);
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO idd_schema_history (version, description, applied_at) VALUES (?, ?, ?)")) {
                        statement.setInt(1, migration.version());
                        statement.setString(2, migration.description());
                        statement.setLong(3, System.currentTimeMillis());
                        statement.executeUpdate();
                    }
                    logging.info("Applied database migration.", java.util.Map.of(
                            "version", migration.version(), "description", migration.description()));
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    exception.addSuppressed(rollbackFailure);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            logging.error("Database migration failed; no schema changes were committed.", exception);
            throw new IllegalStateException("Database migration failed.", exception);
        }
    }

    private void createHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS idd_schema_history (
                        version INTEGER PRIMARY KEY,
                        description VARCHAR(255) NOT NULL,
                        applied_at BIGINT NOT NULL
                    )
                    """);
        }
    }

    private int currentVersion(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COALESCE(MAX(version), 0) FROM idd_schema_history")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static void createInitialSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (String sql : List.of(
                    """
                    CREATE TABLE IF NOT EXISTS dungeon_definitions (
                        id VARCHAR(36) PRIMARY KEY,
                        dungeon_key VARCHAR(128) NOT NULL UNIQUE,
                        display_name VARCHAR(255) NOT NULL,
                        template_world VARCHAR(255) NOT NULL,
                        serialized_data TEXT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS active_instances (
                        id VARCHAR(36) PRIMARY KEY,
                        dungeon_id VARCHAR(36) NOT NULL,
                        world_name VARCHAR(255) NOT NULL UNIQUE,
                        state VARCHAR(32) NOT NULL,
                        created_at BIGINT NOT NULL,
                        completed_at BIGINT
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS parties (
                        id VARCHAR(36) PRIMARY KEY,
                        leader_id VARCHAR(36) NOT NULL,
                        state VARCHAR(32) NOT NULL,
                        serialized_members TEXT NOT NULL,
                        created_at BIGINT NOT NULL
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS player_progress (
                        player_id VARCHAR(36) NOT NULL,
                        instance_id VARCHAR(36) NOT NULL,
                        state VARCHAR(32) NOT NULL,
                        serialized_data TEXT NOT NULL,
                        updated_at BIGINT NOT NULL,
                        PRIMARY KEY (player_id, instance_id)
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS statistics (
                        player_id VARCHAR(36) NOT NULL,
                        statistic_key VARCHAR(128) NOT NULL,
                        statistic_value BIGINT NOT NULL,
                        PRIMARY KEY (player_id, statistic_key)
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS rewards (
                        id VARCHAR(36) PRIMARY KEY,
                        instance_id VARCHAR(36) NOT NULL,
                        player_id VARCHAR(36) NOT NULL,
                        reward_type VARCHAR(64) NOT NULL,
                        payload TEXT NOT NULL,
                        created_at BIGINT NOT NULL
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS cooldowns (
                        player_id VARCHAR(36) NOT NULL,
                        cooldown_key VARCHAR(128) NOT NULL,
                        expires_at BIGINT NOT NULL,
                        PRIMARY KEY (player_id, cooldown_key)
                    )
                    """,
                    """
                    CREATE TABLE IF NOT EXISTS runtime_state (
                        instance_id VARCHAR(36) PRIMARY KEY,
                        state VARCHAR(32) NOT NULL,
                        serialized_data TEXT NOT NULL,
                        updated_at BIGINT NOT NULL
                    )
                    """
            )) {
                statement.executeUpdate(sql);
            }
        }
    }

    @FunctionalInterface
    private interface MigrationAction {
        void apply(Connection connection) throws SQLException;
    }

    private record Migration(int version, String description, MigrationAction action) {
    }
}