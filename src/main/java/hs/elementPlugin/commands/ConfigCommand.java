package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final ConfigManager configManager;

    public ConfigCommand(ElementPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
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
            case "reload" -> {
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                return true;
            }
            case "view" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /config view <section>");
                    sender.sendMessage(ChatColor.YELLOW + "Sections: mana, abilities, passives, combat, items, gui, debug");
                    return true;
                }
                viewSection(sender, args[1]);
                return true;
            }
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /config set <path> <value>");
                    return true;
                }
                setConfigValue(sender, args[1], args[2]);
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Config Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/config reload - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/config view <section> - View config section");
        sender.sendMessage(ChatColor.YELLOW + "/config set <path> <value> - Set config value");
    }

    private void viewSection(CommandSender sender, String section) {
        sender.sendMessage(ChatColor.GOLD + "=== Config: " + section + " ===");

        switch (section.toLowerCase()) {
            case "mana" -> {
                sender.sendMessage(ChatColor.YELLOW + "Max: " + ChatColor.WHITE + configManager.getMaxMana());
                sender.sendMessage(ChatColor.YELLOW + "Regen/Second: " + ChatColor.WHITE + configManager.getManaRegenPerSecond());
                sender.sendMessage(ChatColor.YELLOW + "Creative Infinite: " + ChatColor.WHITE + configManager.isCreativeInfiniteMana());
            }
            case "abilities" -> {
                for (ElementType type : ElementType.values()) {
                    sender.sendMessage(ChatColor.AQUA + type.name() + ":");
                    sender.sendMessage("  Ability 1: " + configManager.getAbility1Cost(type) + " mana, " +
                            configManager.getAbility1Cooldown(type) + "s cooldown");
                    sender.sendMessage("  Ability 2: " + configManager.getAbility2Cost(type) + " mana, " +
                            configManager.getAbility2Cooldown(type) + "s cooldown");
                }
            }
            case "passives" -> {
                sender.sendMessage(ChatColor.GREEN + "Life:");
                sender.sendMessage("  Max Health: " + configManager.getLifeMaxHealth());
                sender.sendMessage("  Crop Growth Radius: " + configManager.getLifeCropGrowthRadius());
                sender.sendMessage(ChatColor.DARK_PURPLE + "Death:");
                sender.sendMessage("  Hunger Radius: " + configManager.getDeathHungerRadius());
                sender.sendMessage(ChatColor.AQUA + "Frost:");
                sender.sendMessage("  Speed on Leather Boots: " + configManager.getFrostSpeedOnLeatherBoots());
                sender.sendMessage("  Speed on Ice: " + configManager.getFrostSpeedOnIce());
            }
            case "combat" -> {
                sender.sendMessage(ChatColor.YELLOW + "Trust Prevents Damage: " + ChatColor.WHITE + configManager.isTrustPreventsDamage());
                sender.sendMessage(ChatColor.YELLOW + "Hellish Flames Duration: " + ChatColor.WHITE + configManager.getHellishFlamesDuration());
                sender.sendMessage(ChatColor.YELLOW + "Frost Nova Freeze: " + ChatColor.WHITE + configManager.getFrostNovaFreezeDuration());
                sender.sendMessage(ChatColor.YELLOW + "Water Prison: " + ChatColor.WHITE + configManager.getWaterPrisonDuration());
            }
            case "items" -> {
                sender.sendMessage(ChatColor.YELLOW + "Upgraders Drop on Death: " + ChatColor.WHITE + configManager.isUpgradersDropOnDeath());
                sender.sendMessage(ChatColor.YELLOW + "Element Items One Per Player: " + ChatColor.WHITE + configManager.isElementItemsOnePerPlayer());
            }
            case "gui" -> {
                sender.sendMessage(ChatColor.YELLOW + "Force Element Selection: " + ChatColor.WHITE + configManager.isForceElementSelection());
                sender.sendMessage(ChatColor.YELLOW + "Reopen on Close: " + ChatColor.WHITE + configManager.isReopenOnCloseWithoutSelection());
            }
            case "debug" -> {
                sender.sendMessage(ChatColor.YELLOW + "Log Ability Usage: " + ChatColor.WHITE + configManager.isLogAbilityUsage());
                sender.sendMessage(ChatColor.YELLOW + "Log Element Assignment: " + ChatColor.WHITE + configManager.isLogElementAssignment());
                sender.sendMessage(ChatColor.YELLOW + "Log Mana Changes: " + ChatColor.WHITE + configManager.isLogManaChanges());
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown section. Valid: mana, abilities, passives, combat, items, gui, debug");
        }
    }

    private void setConfigValue(CommandSender sender, String path, String value) {
        try {
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                configManager.getRawConfig().set(path, Boolean.parseBoolean(value));
            } else if (value.matches("-?\\d+")) {
                configManager.getRawConfig().set(path, Integer.parseInt(value));
            } else if (value.matches("-?\\d+\\.\\d+")) {
                configManager.getRawConfig().set(path, Double.parseDouble(value));
            } else {
                configManager.getRawConfig().set(path, value);
            }

            plugin.saveConfig();
            configManager.reload();
            sender.sendMessage(ChatColor.GREEN + "Set " + path + " to " + value);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error setting value: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "view", "set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("view")) {
            return Arrays.asList("mana", "abilities", "passives", "combat", "items", "gui", "debug").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}