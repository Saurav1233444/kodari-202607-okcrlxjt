package com.kodari.instanceddungeons.database.connection;

import com.kodari.instanceddungeons.config.ConfigurationService;
import com.kodari.instanceddungeons.database.migration.MigrationRunner;
import com.kodari.instanceddungeons.database.repository.RepositoryRegistry;
import com.kodari.instanceddungeons.database.transaction.TransactionManager;
import com.kodari.instanceddungeons.lifecycle.LifecycleComponent;
import com.kodari.instanceddungeons.logging.LoggingService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DatabaseService implements LifecycleComponent {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final LoggingService logging;
    private final ExecutorService executor;
    private final AtomicBoolean stopping = new AtomicBoolean();
    private final CompletableFuture<RepositoryRegistry> ready = new CompletableFuture<>();
    private volatile HikariDataSource dataSource;

    public DatabaseService(JavaPlugin plugin, ConfigurationService configuration, LoggingService logging) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.logging = Objects.requireNonNull(logging, "logging");
        this.executor = Executors.newFixedThreadPool(
                Math.max(2, configuration.databasePoolSize()),
                runnable -> {
                    Thread thread = new Thread(runnable, "InstancedDungeons-Database");
                    thread.setDaemon(true);
                    return thread;
                }
        );
    }

    @Override
    public void start() {
        CompletableFuture.runAsync(this::initialize, executor).whenComplete((ignored, throwable) -> {
            if (throwable != null && !ready.isDone() && !stopping.get()) {
                ready.completeExceptionally(throwable);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    logging.error("Database startup failed; disabling plugin.", throwable);
                    plugin.getServer().getPluginManager().disablePlugin(plugin);
                });
            }
        });
    }

    private void initialize() {
        if (stopping.get()) {
            throw new IllegalStateException("Database startup was cancelled.");
        }
        try {
            HikariConfig hikari = DatabaseConfiguration.create(plugin, configuration);
            HikariDataSource pool = new HikariDataSource(hikari);
            dataSource = pool;
            new MigrationRunner(pool, logging).migrate();
            if (stopping.get()) {
                throw new IllegalStateException("Database startup was cancelled.");
            }
            ready.complete(new RepositoryRegistry(pool, new TransactionManager(pool), executor));
            logging.info("Database initialized.", java.util.Map.of("backend", configuration.storageBackend()));
        } catch (RuntimeException exception) {
            closePool();
            throw exception;
        }
    }

    public CompletableFuture<RepositoryRegistry> ready() {
        return ready;
    }

    public DataSource dataSource() {
        HikariDataSource pool = dataSource;
        if (pool == null || pool.isClosed()) {
            throw new IllegalStateException("Database is not ready.");
        }
        return pool;
    }

    @Override
    public void stop() {
        if (!stopping.compareAndSet(false, true)) {
            return;
        }
        ready.cancel(false);
        executor.shutdownNow();
        closePool();
    }

    private void closePool() {
        HikariDataSource pool = dataSource;
        if (pool != null) {
            dataSource = null;
            pool.close();
        }
    }
}