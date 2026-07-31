package com.kodari.instanceddungeons.commands;

import com.kodari.instanceddungeons.config.ConfigurationService;
import com.kodari.instanceddungeons.lifecycle.LifecycleManager;
import com.kodari.instanceddungeons.logging.LoggingService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collections;
import java.util.List;

/**
 * Administrative bootstrap command used for runtime configuration reloads.
 */
public final class InstancedDungeonsCommand implements CommandExecutor, TabCompleter {
    private final JavaPlugin plugin;
    private final ConfigurationService configuration;
    private final LoggingService logging;
    private final LifecycleManager lifecycle;

    public InstancedDungeonsCommand(
            JavaPlugin plugin,
            ConfigurationService configuration,
            LoggingService logging,
            LifecycleManager lifecycle
    ) {
        this.plugin = plugin;
        this.configuration = configuration;
        this.logging = logging;
        this.lifecycle = lifecycle;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(Component.text("Usage: /" + label + " reload", NamedTextColor.YELLOW));
            return true;
        }

        if (!sender.hasPermission("instanceddungeons.admin.reload")) {
            sender.sendMessage(Component.text("You do not have permission to reload InstancedDungeons.", NamedTextColor.RED));
            return true;
        }

        try {
            lifecycle.reload();
            sender.sendMessage(Component.text("InstancedDungeons configuration reloaded.", NamedTextColor.GREEN));
            logging.info("Configuration reloaded.", java.util.Map.of("source", sender.getName()));
        } catch (Exception exception) {
            logging.error("Configuration reload failed.", exception);
            sender.sendMessage(Component.text(
                    "Configuration reload failed. Check the server log.",
                    NamedTextColor.RED
            ));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (args.length == 1 && sender.hasPermission("instanceddungeons.admin.reload")) {
            String input = args[0].toLowerCase(java.util.Locale.ROOT);
            if ("reload".startsWith(input)) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }
}