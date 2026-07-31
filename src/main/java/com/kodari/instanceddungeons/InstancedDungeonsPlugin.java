package com.kodari.instanceddungeons;

import com.kodari.instanceddungeons.commands.InstancedDungeonsCommand;
import com.kodari.instanceddungeons.config.ConfigurationService;
import com.kodari.instanceddungeons.database.repository.RepositoryRegistry;
import com.kodari.instanceddungeons.lifecycle.LifecycleManager;
import com.kodari.instanceddungeons.logging.LoggingService;
import com.kodari.instanceddungeons.database.connection.DatabaseService;
import com.kodari.instanceddungeons.services.ServiceRegistry;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point and composition root.
 */
public final class InstancedDungeonsPlugin extends JavaPlugin {
    private ConfigurationService configurationService;
    private LoggingService loggingService;
    private ServiceRegistry serviceRegistry;
    private LifecycleManager lifecycleManager;
    private DatabaseService databaseService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigurationService configuration = new ConfigurationService(this);
        LoggingService logging = new LoggingService(this, configuration);
        DatabaseService database = new DatabaseService(this, configuration, logging);
        ServiceRegistry services = new ServiceRegistry();
        LifecycleManager lifecycle = new LifecycleManager(logging);

        services.register(ConfigurationService.class, configuration);
        services.register(LoggingService.class, logging);
        services.register(DatabaseService.class, database);
        services.register(ServiceRegistry.class, services);
        services.register(LifecycleManager.class, lifecycle);

        lifecycle.register(configuration);
        lifecycle.register(logging);
        lifecycle.register(database);

        try {
            lifecycle.start();
        } catch (Exception exception) {
            logging.error("Plugin startup failed.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.configurationService = configuration;
        this.loggingService = logging;
        this.serviceRegistry = services;
        this.lifecycleManager = lifecycle;

        PluginCommand command = getCommand("instanceddungeons");
        if (command == null) {
            logging.error("The instanceddungeons command is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        InstancedDungeonsCommand commandHandler = new InstancedDungeonsCommand(
                this,
                configuration,
                logging,
                lifecycle
        );
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        logging.info("InstancedDungeons enabled.");
    }

    @Override
    public void onDisable() {
        if (lifecycleManager != null) {
            lifecycleManager.stop();
        }

        if (loggingService != null) {
            loggingService.info("InstancedDungeons disabled.");
        } else {
            getLogger().info("InstancedDungeons disabled.");
        }
    }

    /**
     * Returns the active configuration service.
     *
     * @return configuration service
     */
    public ConfigurationService configuration() {
        return configurationService;
    }

    /**
     * Returns the service registry used by this plugin.
     *
     * @return service registry
     */
    public ServiceRegistry services() {
        return serviceRegistry;
    }

    /**
     * Returns the structured plugin logger.
     *
     * @return logging service
     */
    public LoggingService logging() {
        return loggingService;
    }

    /**
     * Returns the database repositories after asynchronous database startup completes.
     *
     * @return repository readiness future
     */
    public java.util.concurrent.CompletableFuture<RepositoryRegistry> repositories() {
        return serviceRegistry.require(DatabaseService.class).ready();
    }
}