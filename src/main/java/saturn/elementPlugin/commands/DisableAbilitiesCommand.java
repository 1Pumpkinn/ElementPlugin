package saturn.elementPlugin.commands;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.regions.DisabledRegion;
import saturn.elementPlugin.regions.DisabledRegionsManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command to manage disabled ability regions
 * Usage: /disableAbilities <create|remove|list|info|reload> [args]
 */
public class DisableAbilitiesCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final DisabledRegionsManager regionsManager;

    // Store player selections for pos1 and pos2
    private final Map<UUID, Location> pos1Selections = new HashMap<>();
    private final Map<UUID, Location> pos2Selections = new HashMap<>();

    public DisableAbilitiesCommand(ElementPlugin plugin, DisabledRegionsManager regionsManager) {
        this.plugin = plugin;
        this.regionsManager = regionsManager;
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
            case "create" -> handleCreate(sender, args);
            case "remove", "delete" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> handleReload(sender);
            case "pos1", "1" -> handlePos1(sender);
            case "pos2", "2" -> handlePos2(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can create regions!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /disableAbilities create <name>");
            sender.sendMessage(ChatColor.YELLOW + "First set pos1 and pos2 with /disableAbilities pos1 and pos2");
            return;
        }

        String regionName = args[1];

        // Check if region already exists
        if (regionsManager.regionExists(regionName)) {
            sender.sendMessage(ChatColor.RED + "A region with that name already exists!");
            return;
        }

        // Check if positions are set
        Location pos1 = pos1Selections.get(player.getUniqueId());
        Location pos2 = pos2Selections.get(player.getUniqueId());

        if (pos1 == null || pos2 == null) {
            sender.sendMessage(ChatColor.RED + "You must set both pos1 and pos2 first!");
            sender.sendMessage(ChatColor.YELLOW + "Use /disableAbilities pos1 and /disableAbilities pos2");
            return;
        }

        // Create the region
        if (regionsManager.addRegion(regionName, pos1, pos2)) {
            DisabledRegion region = regionsManager.getRegion(regionName);

            player.sendMessage(Component.text("✓ Created disabled region: ", NamedTextColor.GREEN)
                    .append(Component.text(regionName, NamedTextColor.AQUA)));
            player.sendMessage(Component.text("  World: ", NamedTextColor.GRAY)
                    .append(Component.text(region.getWorldName(), NamedTextColor.WHITE)));
            player.sendMessage(Component.text("  Volume: ", NamedTextColor.GRAY)
                    .append(Component.text(region.getVolume() + " blocks", NamedTextColor.WHITE)));

            // Clear selections
            pos1Selections.remove(player.getUniqueId());
            pos2Selections.remove(player.getUniqueId());
        } else {
            sender.sendMessage(ChatColor.RED + "Failed to create region!");
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /disableAbilities remove <name>");
            return;
        }

        String regionName = args[1];

        if (regionsManager.removeRegion(regionName)) {
            sender.sendMessage(Component.text("✓ Removed disabled region: ", NamedTextColor.YELLOW)
                    .append(Component.text(regionName, NamedTextColor.AQUA)));
        } else {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' not found!");
        }
    }

    private void handleList(CommandSender sender) {
        Collection<DisabledRegion> regions = regionsManager.getAllRegions();

        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No disabled regions configured.");
            return;
        }

        sender.sendMessage(Component.text("━━━ Disabled Ability Regions ━━━", NamedTextColor.GOLD));

        for (DisabledRegion region : regions) {
            sender.sendMessage(Component.text("• ", NamedTextColor.GRAY)
                    .append(Component.text(region.getName(), NamedTextColor.AQUA))
                    .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(region.getWorldName(), NamedTextColor.WHITE))
                    .append(Component.text(" (" + region.getVolume() + " blocks)", NamedTextColor.GRAY)));
        }

        sender.sendMessage(Component.text("Total: " + regions.size() + " region(s)", NamedTextColor.GRAY));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /disableAbilities info <name>");
            return;
        }

        String regionName = args[1];
        DisabledRegion region = regionsManager.getRegion(regionName);

        if (region == null) {
            sender.sendMessage(ChatColor.RED + "Region '" + regionName + "' not found!");
            return;
        }

        sender.sendMessage(Component.text("━━━ Region Info: " + region.getName() + " ━━━", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("World: ", NamedTextColor.YELLOW)
                .append(Component.text(region.getWorldName(), NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Position 1: ", NamedTextColor.YELLOW)
                .append(Component.text(region.getMinX() + ", " + region.getMinY() + ", " + region.getMinZ(),
                        NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Position 2: ", NamedTextColor.YELLOW)
                .append(Component.text(region.getMaxX() + ", " + region.getMaxY() + ", " + region.getMaxZ(),
                        NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("Volume: ", NamedTextColor.YELLOW)
                .append(Component.text(region.getVolume() + " blocks", NamedTextColor.WHITE)));
    }

    private void handleReload(CommandSender sender) {
        regionsManager.reload();
        sender.sendMessage(Component.text("✓ Reloaded disabled regions", NamedTextColor.GREEN));
    }

    private void handlePos1(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set positions!");
            return;
        }

        Location location = player.getLocation();
        pos1Selections.put(player.getUniqueId(), location);

        player.sendMessage(Component.text("✓ Position 1 set to: ", NamedTextColor.GREEN)
                .append(Component.text(location.getBlockX() + ", " + location.getBlockY() + ", " +
                        location.getBlockZ(), NamedTextColor.WHITE)));

        // Show pos2 status
        if (pos2Selections.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("  Position 2 already set. Use /disableAbilities create <name> to create the region.",
                    NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("  Now set Position 2 with /disableAbilities pos2",
                    NamedTextColor.GRAY));
        }
    }

    private void handlePos2(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can set positions!");
            return;
        }

        Location location = player.getLocation();
        pos2Selections.put(player.getUniqueId(), location);

        player.sendMessage(Component.text("✓ Position 2 set to: ", NamedTextColor.GREEN)
                .append(Component.text(location.getBlockX() + ", " + location.getBlockY() + ", " +
                        location.getBlockZ(), NamedTextColor.WHITE)));

        // Show pos1 status
        if (pos1Selections.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("  Both positions set. Use /disableAbilities create <name> to create the region.",
                    NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("  Now set Position 1 with /disableAbilities pos1",
                    NamedTextColor.GRAY));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("━━━ Disabled Abilities Commands ━━━", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/disableAbilities pos1", NamedTextColor.AQUA)
                .append(Component.text(" - Set position 1 at your location", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities pos2", NamedTextColor.AQUA)
                .append(Component.text(" - Set position 2 at your location", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities create <name>", NamedTextColor.AQUA)
                .append(Component.text(" - Create a disabled region", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities remove <name>", NamedTextColor.AQUA)
                .append(Component.text(" - Remove a region", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities list", NamedTextColor.AQUA)
                .append(Component.text(" - List all regions", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities info <name>", NamedTextColor.AQUA)
                .append(Component.text(" - View region details", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/disableAbilities reload", NamedTextColor.AQUA)
                .append(Component.text(" - Reload regions from disk", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("create", "remove", "list", "info", "reload", "pos1", "pos2")
                    .stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") ||
                    args[0].equalsIgnoreCase("delete") ||
                    args[0].equalsIgnoreCase("info")) {
                return new ArrayList<>(regionsManager.getRegionNames()).stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}