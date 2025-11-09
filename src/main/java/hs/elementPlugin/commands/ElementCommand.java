package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ElementCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final DataStore dataStore;
    private final ElementManager elementManager;

    public ElementCommand(ElementPlugin plugin) {
        this.plugin = plugin;
        this.dataStore = plugin.getDataStore();
        this.elementManager = plugin.getElementManager();
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
            case "set":
                return handleSet(sender, args);
            case "debug":
                return handleDebug(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /element set <player> <air|water|fire|earth|life|death|metal|frost>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return true;
        }

        ElementType elementType;
        try {
            elementType = ElementType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid element type. Use: air, water, fire, earth, life, death, metal, or frost");
            return true;
        }

        plugin.getLogger().info("[ElementCommand] Setting element for " + target.getName() + " (" + target.getUniqueId() + ") to " + elementType.name());

        // Set element using ElementManager
        elementManager.setElement(target, elementType);

        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s element to " + ChatColor.AQUA + elementType.name());
        target.sendMessage(ChatColor.GREEN + "Your element has been set to " + ChatColor.AQUA + elementType.name() + ChatColor.GREEN + " by an admin.");

        return true;
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /element debug <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Element Debug for " + target.getName() + " ===");

        // Check ElementManager
        ElementType managerElement = elementManager.getPlayerElement(target);
        sender.sendMessage(ChatColor.YELLOW + "ElementManager reports: " + (managerElement != null ? managerElement.name() : "null"));

        // Force cache invalidation and reload
        dataStore.invalidateCache(target.getUniqueId());
        ElementType reloadedElement = elementManager.getPlayerElement(target);
        sender.sendMessage(ChatColor.YELLOW + "After cache invalidation: " + (reloadedElement != null ? reloadedElement.name() : "null"));

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Element Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/element set <player> <element> - Set player's element");
        sender.sendMessage(ChatColor.YELLOW + "/element debug <player> - Debug player's element data");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            return new ArrayList<>();
        }

        // First argument: show subcommands
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("set", "debug");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Second argument: show online player names for both set and debug
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            if (subcommand.equals("set") || subcommand.equals("debug")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        // Third argument for /element set <player> <element>
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> elements = Arrays.asList("air", "water", "fire", "earth", "life", "death", "metal", "frost");
            return elements.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}