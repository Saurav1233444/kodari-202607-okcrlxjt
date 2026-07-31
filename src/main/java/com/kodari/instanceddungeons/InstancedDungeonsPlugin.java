package com.kodari.instanceddungeons;

import com.kodari.instanceddungeons.commands.InstancedDungeonsCommand;
import com.kodari.instanceddungeons.config.ConfigurationService;
import com.kodari.instanceddungeons.database.connection.DatabaseService;
import com.kodari.instanceddungeons.database.repository.RepositoryRegistry;
import com.kodari.instanceddungeons.dungeon.definition.YamlDungeonLoader;
import com.kodari.instanceddungeons.editor.EditorSessionManager;
import com.kodari.instanceddungeons.instance.InstanceRecoveryListener;
import com.kodari.instanceddungeons.instance.InstanceRecoveryManager;
import com.kodari.instanceddungeons.instance.InstanceRuntimeManager;
import com.kodari.instanceddungeons.instance.SafeTeleportService;
import com.kodari.instanceddungeons.lifecycle.LifecycleManager;
import com.kodari.instanceddungeons.logging.LoggingService;
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
    private InstanceRuntimeManager runtimeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigurationService configuration = new ConfigurationService(this);
        LoggingService logging             = new LoggingService(this, configuration);
        DatabaseService database           = new DatabaseService(this, configuration, logging);
        ServiceRegistry services           = new ServiceRegistry();
        LifecycleManager lifecycle         = new LifecycleManager(logging);

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
        this.loggingService       = logging;
        this.serviceRegistry      = services;
        this.lifecycleManager     = lifecycle;

        // Phase 3 — wire runtime services once database is ready.
        SafeTeleportService teleportService = new SafeTeleportService();
        services.register(SafeTeleportService.class, teleportService);

        YamlDungeonLoader dungeonLoader = new YamlDungeonLoader(getDataFolder(), getLogger());
        services.register(YamlDungeonLoader.class, dungeonLoader);

        EditorSessionManager editorSessions = new EditorSessionManager();
        services.register(EditorSessionManager.class, editorSessions);

        database.ready().whenCompleteAsync((repositories, error) -> {
            if (error != null) {
                // DatabaseService already disables the plugin on failure; nothing more to do here.
                return;
            }
            wirePhase3(repositories, teleportService, logging);
        });

        PluginCommand command = getCommand("instanceddungeons");
        if (command == null) {
            logging.error("The instanceddungeons command is missing from plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        InstancedDungeonsCommand commandHandler = new InstancedDungeonsCommand(
                this, configuration, logging, lifecycle);
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        logging.info("InstancedDungeons enabled.");
    }

    private void wirePhase3(RepositoryRegistry repositories, SafeTeleportService teleportService,
                             LoggingService logging) {
        InstanceRuntimeManager runtime = new InstanceRuntimeManager(this, teleportService, repositories, logging);
        this.runtimeManager = runtime;
        serviceRegistry.register(InstanceRuntimeManager.class, runtime);

        // Start runtime on the main thread (Bukkit scheduler requirement)
        getServer().getScheduler().runTask(this, () -> {
            runtime.start();

            // Register recovery listener
            InstanceRecoveryListener recoveryListener = new InstanceRecoveryListener(teleportService, runtime);
            getServer().getPluginManager().registerEvents(recoveryListener, this);

            // Run crash recovery
            new InstanceRecoveryManager(this, repositories, teleportService, logging).recover();

            logging.info("Phase 3 runtime services started.");
        });
    }

    @Override
    public void onDisable() {
        if (runtimeManager != null) {
            runtimeManager.stop();
        }
        if (lifecycleManager != null) {
            lifecycleManager.stop();
        }
        if (loggingService != null) {
            loggingService.info("InstancedDungeons disabled.");
        } else {
            getLogger().info("InstancedDungeons disabled.");
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public ConfigurationService configuration()     { return configurationService; }
    public ServiceRegistry services()               { return serviceRegistry; }
    public LoggingService logging()                 { return loggingService; }
    public InstanceRuntimeManager runtimeManager()  { return runtimeManager; }

    public java.util.concurrent.CompletableFuture<RepositoryRegistry> repositories() {
        return serviceRegistry.require(DatabaseService.class).ready();
    }
}