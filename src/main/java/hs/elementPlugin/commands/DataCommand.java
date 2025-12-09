package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Admin command for managing data storage
 * Usage: /data <save|backup|stats|reload>
 */
public class DataCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final DataStore dataStore;

    public DataCommand(ElementPlugin plugin) {
        this.plugin = plugin;
        this.dataStore = plugin.getDataStore();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "save" -> handleSave(sender);
            case "backup" -> handleBackup(sender);
            case "stats" -> handleStats(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSave(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Saving all player data...");

        long startTime = System.currentTimeMillis();
        dataStore.flushAll();
        long duration = System.currentTimeMillis() - startTime;

        sender.sendMessage(ChatColor.GREEN + "✓ All data saved successfully! (" + duration + "ms)");
    }

    private void handleBackup(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Creating backup...");

        boolean success = dataStore.createBackup();

        if (success) {
            sender.sendMessage(ChatColor.GREEN + "✓ Backup created successfully!");
        } else {
            sender.sendMessage(ChatColor.RED + "✗ Failed to create backup. Check console for errors.");
        }
    }

    private void handleStats(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Data Store Statistics ===");

        Map<String, Object> stats = dataStore.getStatistics();

        sender.sendMessage(ChatColor.YELLOW + "Cached Players: " + ChatColor.WHITE + stats.get("cached_players"));
        sender.sendMessage(ChatColor.YELLOW + "Unsaved Changes: " + ChatColor.WHITE + stats.get("dirty_players"));
        sender.sendMessage(ChatColor.YELLOW + "Last Save: " + ChatColor.WHITE + stats.get("last_save"));
        sender.sendMessage(ChatColor.YELLOW + "File Size: " + ChatColor.WHITE +
                (Long) stats.get("player_file_size") / 1024 + " KB");
        sender.sendMessage(ChatColor.YELLOW + "Backups: " + ChatColor.WHITE + stats.get("backup_count"));
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Reloading player data is not recommended while players are online!");
        sender.sendMessage(ChatColor.YELLOW + "Use /data save instead to flush changes to disk.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Data Management Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/data save " + ChatColor.GRAY + "- Force save all player data");
        sender.sendMessage(ChatColor.YELLOW + "/data backup " + ChatColor.GRAY + "- Create a manual backup");
        sender.sendMessage(ChatColor.YELLOW + "/data stats " + ChatColor.GRAY + "- View data store statistics");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("save", "backup", "stats");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        return new ArrayList<>();
    }
}