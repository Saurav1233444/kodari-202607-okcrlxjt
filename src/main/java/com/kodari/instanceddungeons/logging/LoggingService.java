package com.kodari.instanceddungeons.logging;

import com.kodari.instanceddungeons.config.ConfigurationService;
import com.kodari.instanceddungeons.lifecycle.LifecycleComponent;
import com.kodari.instanceddungeons.lifecycle.Reloadable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.logging.Level;

/**
 * Structured logger with runtime debug configuration.
 */
public final class LoggingService implements LifecycleComponent, Reloadable {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;

    public LoggingService(JavaPlugin plugin, ConfigurationService configuration) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
    }

    @Override
    public void start() {
        info("Logging service started.");
    }

    @Override
    public void stop() {
        info("Logging service stopped.");
    }

    @Override
    public void reload() {
        debug("Logging configuration reloaded.", Map.of("debug", configuration.debug()));
    }

    /** Logs an informational message. */
    public void info(String message) {
        log(Level.INFO, message, Map.of());
    }

    /** Logs an informational message with structured fields. */
    public void info(String message, Map<String, ?> fields) {
        log(Level.INFO, message, fields);
    }

    /** Logs a warning. */
    public void warn(String message) {
        log(Level.WARNING, message, Map.of());
    }

    /** Logs an error message. */
    public void error(String message) {
        plugin.getLogger().log(Level.SEVERE, message);
    }

    /** Logs an error and its cause. */
    public void error(String message, Throwable throwable) {
        plugin.getLogger().log(Level.SEVERE, message, throwable);
    }

    /** Logs a debug message only when debug logging is enabled. */
    public void debug(String message) {
        debug(message, Map.of());
    }

    /** Logs a debug message with structured fields. */
    public void debug(String message, Map<String, ?> fields) {
        if (configuration.debug()) {
            log(Level.INFO, message, fields);
        }
    }

    private void log(Level level, String message, Map<String, ?> fields) {
        StringJoiner joiner = new StringJoiner(" ", message + (fields.isEmpty() ? "" : " "), "");
        fields.forEach((key, value) -> joiner.add(key + "=" + Objects.toString(value)));
        plugin.getLogger().log(level, joiner.toString());
    }
}