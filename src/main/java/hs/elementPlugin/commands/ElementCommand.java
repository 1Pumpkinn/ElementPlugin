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

import java.util.*;
import java.util.stream.Collectors;

public class ElementCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final DataStore dataStore;
    private final ElementManager elementManager;
    private final Map<String, SubCommand> subCommands;

    public ElementCommand(ElementPlugin plugin) {
        this.plugin = plugin;
        this.dataStore = plugin.getDataStore();
        this.elementManager = plugin.getElementManager();
        this.subCommands = initializeSubCommands();
    }

    private Map<String, SubCommand> initializeSubCommands() {
        Map<String, SubCommand> commands = new HashMap<>();
        commands.put("set", new SetCommand());
        commands.put("debug", new DebugCommand());
        return commands;
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

        SubCommand subCommand = subCommands.get(args[0].toLowerCase());
        if (subCommand != null) {
            return subCommand.execute(sender, args);
        }

        sendHelp(sender);
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
            return Collections.emptyList();
        }

        return switch (args.length) {
            case 1 -> filterStartingWith(subCommands.keySet(), args[0]);
            case 2 -> {
                String subCmd = args[0].toLowerCase();
                if (subCommands.containsKey(subCmd)) {
                    yield getOnlinePlayerNames(args[1]);
                }
                yield Collections.emptyList();
            }
            case 3 -> args[0].equalsIgnoreCase("set") ?
                    filterStartingWith(getElementNames(), args[2]) :
                    Collections.emptyList();
            default -> Collections.emptyList();
        };
    }

    private List<String> filterStartingWith(Collection<String> options, String prefix) {
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getOnlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getElementNames() {
        return Arrays.stream(ElementType.values())
                .map(type -> type.name().toLowerCase())
                .collect(Collectors.toList());
    }

    // Inner classes for sub-commands
    private interface SubCommand {
        boolean execute(CommandSender sender, String[] args);
    }

    private class SetCommand implements SubCommand {
        @Override
        public boolean execute(CommandSender sender, String[] args) {
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /element set <player> <element>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
                return true;
            }

            Optional<ElementType> elementType = parseElementType(args[2]);
            if (elementType.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Invalid element. Valid: " + String.join(", ", getElementNames()));
                return true;
            }

            plugin.getLogger().info(String.format("[ElementCommand] Setting element for %s (%s) to %s",
                    target.getName(), target.getUniqueId(), elementType.get().name()));

            elementManager.setElement(target, elementType.get());

            sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s element to " +
                    ChatColor.AQUA + elementType.get().name());
            target.sendMessage(ChatColor.GREEN + "Your element has been set to " +
                    ChatColor.AQUA + elementType.get().name() + ChatColor.GREEN + " by an admin.");

            return true;
        }

        private Optional<ElementType> parseElementType(String input) {
            try {
                return Optional.of(ElementType.valueOf(input.toUpperCase()));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }

    private class DebugCommand implements SubCommand {
        @Override
        public boolean execute(CommandSender sender, String[] args) {
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

            ElementType managerElement = elementManager.getPlayerElement(target);
            sender.sendMessage(ChatColor.YELLOW + "ElementManager reports: " +
                    (managerElement != null ? managerElement.name() : "null"));

            dataStore.invalidateCache(target.getUniqueId());
            ElementType reloadedElement = elementManager.getPlayerElement(target);
            sender.sendMessage(ChatColor.YELLOW + "After cache invalidation: " +
                    (reloadedElement != null ? reloadedElement.name() : "null"));

            return true;
        }
    }
}