package com.kodari.instanceddungeons.database.connection;

import com.kodari.instanceddungeons.config.ConfigurationService;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;

final class DatabaseConfiguration {
    private DatabaseConfiguration() {
    }

    static HikariConfig create(JavaPlugin plugin, ConfigurationService configuration) {
        String backend = configuration.storageBackend();
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("InstancedDungeons-" + backend);
        hikari.setMaximumPoolSize(configuration.databasePoolSize());
        hikari.setMinimumIdle(Math.min(2, configuration.databasePoolSize()));
        hikari.setConnectionTimeout(10_000L);
        hikari.setValidationTimeout(5_000L);
        hikari.setInitializationFailTimeout(10_000L);

        switch (backend) {
            case "SQLITE" -> configureSqlite(plugin, configuration, hikari);
            case "MYSQL" -> configureServer(hikari, "com.mysql.cj.jdbc.Driver",
                    "jdbc:mysql://" + configuration.databaseHost() + ":" + configuration.databasePort()
                            + "/" + configuration.databaseName() + "?useSSL=false&serverTimezone=UTC", configuration);
            case "MARIADB" -> configureServer(hikari, "org.mariadb.jdbc.Driver",
                    "jdbc:mariadb://" + configuration.databaseHost() + ":" + configuration.databasePort()
                            + "/" + configuration.databaseName(), configuration);
            case "POSTGRESQL" -> configureServer(hikari, "org.postgresql.Driver",
                    "jdbc:postgresql://" + configuration.databaseHost() + ":" + configuration.databasePort()
                            + "/" + configuration.databaseName(), configuration);
            default -> throw new IllegalStateException("Unsupported database backend: " + backend);
        }
        return hikari;
    }

    private static void configureSqlite(JavaPlugin plugin, ConfigurationService configuration, HikariConfig hikari) {
        File database = new File(plugin.getDataFolder(), configuration.databaseFile());
        try {
            File parent = database.getParentFile();
            if (parent != null) {
                Files.createDirectories(parent.toPath());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create SQLite database directory.", exception);
        }
        hikari.setDriverClassName("org.sqlite.JDBC");
        hikari.setJdbcUrl("jdbc:sqlite:" + database.getPath());
        hikari.setMaximumPoolSize(1);
        hikari.setMinimumIdle(1);
        hikari.addDataSourceProperty("busy_timeout", "10000");
        hikari.addDataSourceProperty("foreign_keys", "true");
    }

    private static void configureServer(HikariConfig hikari, String driver, String url,
                                        ConfigurationService configuration) {
        hikari.setDriverClassName(driver);
        hikari.setJdbcUrl(url);
        hikari.setUsername(configuration.databaseUsername());
        hikari.setPassword(configuration.databasePassword());
    }
}