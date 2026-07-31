package com.kodari.instanceddungeons.config;

import com.kodari.instanceddungeons.lifecycle.LifecycleComponent;
import com.kodari.instanceddungeons.lifecycle.Reloadable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;

/**
 * Loads and validates the plugin configuration.
 */
public final class ConfigurationService implements LifecycleComponent, Reloadable {
    private static final Set<String> SUPPORTED_BACKENDS = Set.of(
            "SQLITE",
            "MYSQL",
            "MARIADB",
            "POSTGRESQL"
    );

    private final JavaPlugin plugin;

    public ConfigurationService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void start() {
        validate();
    }

    @Override
    public void stop() {
        plugin.getLogger().fine("Configuration service stopped.");
    }

    @Override
    public void reload() {
        plugin.reloadConfig();
        validate();
    }

    /**
     * Validates values required by the runtime.
     */
    public void validate() {
        String backend = storageBackend();
        if (!SUPPORTED_BACKENDS.contains(backend)) {
            throw new IllegalStateException(
                    "Unsupported storage backend '" + backend + "'. Supported values: " + SUPPORTED_BACKENDS
            );
        }

        validatePositive("storage.pool-size", databasePoolSize());
        validatePositive("dungeons.default-time-limit-seconds", defaultTimeLimitSeconds());
        validatePositive("worlds.orphan-timeout-seconds", orphanTimeoutSeconds());
        validatePositive("worlds.unload-delay-ticks", Math.toIntExact(unloadDelayTicks()));
        validatePositive("dungeons.start-countdown-seconds", startCountdownSeconds());
        validatePositive("dungeons.reconnect-timeout-seconds", reconnectTimeoutSeconds());

        if (databasePort() <= 0 || databasePort() > 65535) {
            throw new IllegalStateException("storage.port must be between 1 and 65535");
        }

        if (!"SQLITE".equals(backend)) {
            if (databaseHost().isBlank() || databaseName().isBlank()) {
                throw new IllegalStateException("storage.host and storage.database are required for " + backend);
            }
        }

        if (entryFee() < 0.0D) {
            throw new IllegalStateException("dungeons.entry-fee cannot be negative");
        }
    }

    public String storageBackend() {
        return plugin.getConfig().getString("storage.backend", "SQLITE").toUpperCase(Locale.ROOT);
    }

    public String databaseFile() {
        return plugin.getConfig().getString("storage.database-file", "data/dungeons.db");
    }

    public String databaseHost() {
        return plugin.getConfig().getString("storage.host", "localhost");
    }

    public int databasePort() {
        return plugin.getConfig().getInt("storage.port", 3306);
    }

    public String databaseName() {
        return plugin.getConfig().getString("storage.database", "instanced_dungeons");
    }

    public String databaseUsername() {
        return plugin.getConfig().getString("storage.username", "");
    }

    public String databasePassword() {
        return plugin.getConfig().getString("storage.password", "");
    }

    public int databasePoolSize() {
        return plugin.getConfig().getInt("storage.pool-size", 10);
    }

    public String templateWorld() {
        return plugin.getConfig().getString("worlds.template-world", "dungeon-template");
    }

    public String instanceDirectory() {
        return plugin.getConfig().getString("worlds.instance-directory", "instances");
    }

    public boolean keepTemplateLoaded() {
        return plugin.getConfig().getBoolean("worlds.keep-template-loaded", true);
    }

    public boolean autoDeleteOnComplete() {
        return plugin.getConfig().getBoolean("worlds.auto-delete-on-complete", true);
    }

    public boolean autoDeleteOnFail() {
        return plugin.getConfig().getBoolean("worlds.auto-delete-on-fail", true);
    }

    public int orphanTimeoutSeconds() {
        return plugin.getConfig().getInt("worlds.orphan-timeout-seconds", 300);
    }

    public long unloadDelayTicks() {
        return plugin.getConfig().getLong("worlds.unload-delay-ticks", 20L);
    }

    public int partyMinimumSize() {
        return plugin.getConfig().getInt("parties.minimum-size", 1);
    }

    public int partyMaximumSize() {
        return plugin.getConfig().getInt("parties.maximum-size", 5);
    }

    public int inviteTimeoutSeconds() {
        return plugin.getConfig().getInt("parties.invite-timeout-seconds", 60);
    }

    public int readyCheckTimeoutSeconds() {
        return plugin.getConfig().getInt("parties.ready-check-timeout-seconds", 30);
    }

    public boolean allowPartyChat() {
        return plugin.getConfig().getBoolean("parties.allow-party-chat", true);
    }

    public boolean allowSpectators() {
        return plugin.getConfig().getBoolean("parties.allow-spectators", true);
    }

    public int defaultTimeLimitSeconds() {
        return plugin.getConfig().getInt("dungeons.default-time-limit-seconds", 1800);
    }

    public int startCountdownSeconds() {
        return plugin.getConfig().getInt("dungeons.start-countdown-seconds", 10);
    }

    public double entryFee() {
        return plugin.getConfig().getDouble("dungeons.entry-fee", 0.0D);
    }

    public boolean requireReadyCheck() {
        return plugin.getConfig().getBoolean("dungeons.require-ready-check", true);
    }

    public boolean allowReconnect() {
        return plugin.getConfig().getBoolean("dungeons.allow-reconnect", true);
    }

    public int reconnectTimeoutSeconds() {
        return plugin.getConfig().getInt("dungeons.reconnect-timeout-seconds", 120);
    }

    public boolean debug() {
        return plugin.getConfig().getBoolean("logging.debug", false);
    }

    public boolean performanceMetrics() {
        return plugin.getConfig().getBoolean("logging.performance-metrics", true);
    }

    private void validatePositive(String path, int value) {
        if (value <= 0) {
            throw new IllegalStateException(path + " must be greater than zero");
        }
    }
}