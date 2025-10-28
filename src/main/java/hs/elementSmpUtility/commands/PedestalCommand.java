package hs.elementSmpUtility.commands;

import hs.elementSmpUtility.storage.pedestal.PedestalDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Admin command for managing pedestals
 */
public class PedestalCommand implements CommandExecutor, TabCompleter {

    private final PedestalDataStorage storage;
    private final PedestalOwnerStorage ownerStorage;

    public PedestalCommand(PedestalDataStorage storage, PedestalOwnerStorage ownerStorage) {
        this.storage = storage;
        this.ownerStorage = ownerStorage;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("elementsmp.pedestal.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "info":
                handleInfo(sender);
                break;

            case "reload":
                handleReload(sender);
                break;

            case "check":
                handleCheck(sender);
                break;

            case "claim":
                handleClaim(sender);
                break;

            case "transfer":
                handleTransfer(sender, args);
                break;

            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleInfo(CommandSender sender) {
        int count = storage.getYmlStorage().getPedestalCount();

        sender.sendMessage(Component.text("=== Pedestal Storage Info ===")
                .color(TextColor.color(0x55FFFF)));
        sender.sendMessage(Component.text("Total pedestals in YAML: " + count)
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("Storage: Dual (Chunk PDC + YAML backup)")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("YAML file: plugins/ElementSmpUtility/pedestals.yml")
                .color(TextColor.color(0xAAAAAA)));
        sender.sendMessage(Component.text("Owner storage: Dual (Chunk PDC + YAML backup)")
                .color(TextColor.color(0x55FF55)));
    }

    private void handleReload(CommandSender sender) {
        storage.getYmlStorage().reload();

        sender.sendMessage(Component.text("Pedestal data reloaded from YAML!")
                .color(TextColor.color(0x55FF55)));
        sender.sendMessage(Component.text("Found " + storage.getYmlStorage().getPedestalCount() + " pedestals")
                .color(TextColor.color(0xFFFFFF)));
    }

    private void handleCheck(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(Component.text("You must be looking at a pedestal!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Location loc = targetBlock.getLocation();
        UUID owner = ownerStorage.getOwner(loc);

        if (owner == null) {
            player.sendMessage(Component.text("This pedestal has NO OWNER!")
                    .color(TextColor.color(0xFF5555)));
            player.sendMessage(Component.text("Use /pedestal claim to claim it")
                    .color(TextColor.color(0xFFAA00)));
            return;
        }

        String ownerName = ownerStorage.getOwnerName(loc);
        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(owner);

        player.sendMessage(Component.text("=== Pedestal Ownership ===")
                .color(TextColor.color(0x55FFFF)));
        player.sendMessage(Component.text("Owner: " + ownerName)
                .color(TextColor.color(0xFFFFFF)));
        player.sendMessage(Component.text("UUID: " + owner.toString())
                .color(TextColor.color(0xAAAAAA)));
        player.sendMessage(Component.text("Online: " + (ownerPlayer.isOnline() ? "Yes" : "No"))
                .color(TextColor.color(ownerPlayer.isOnline() ? 0x55FF55 : 0xFF5555)));

        // Show if player is the owner
        if (owner.equals(player.getUniqueId())) {
            player.sendMessage(Component.text("You OWN this pedestal")
                    .color(TextColor.color(0x55FF55)));
        } else {
            player.sendMessage(Component.text("You do NOT own this pedestal")
                    .color(TextColor.color(0xFF5555)));
        }
    }

    private void handleClaim(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(Component.text("You must be looking at a pedestal!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Location loc = targetBlock.getLocation();
        UUID currentOwner = ownerStorage.getOwner(loc);

        // Check if pedestal already has an owner
        if (currentOwner != null) {
            String ownerName = ownerStorage.getOwnerName(loc);
            player.sendMessage(Component.text("This pedestal is already owned by " + ownerName + "!")
                    .color(TextColor.color(0xFF5555)));
            player.sendMessage(Component.text("Use /pedestal transfer <player> to change ownership")
                    .color(TextColor.color(0xFFAA00)));
            return;
        }

        // Claim the pedestal
        ownerStorage.setOwner(loc, player.getUniqueId());
        player.sendMessage(Component.text("Pedestal claimed successfully!")
                .color(TextColor.color(0x55FF55)));
        player.sendMessage(Component.text("You are now the owner of this pedestal")
                .color(TextColor.color(0xFFFFFF)));
    }

    private void handleTransfer(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /pedestal transfer <player|uuid>")
                    .color(TextColor.color(0xFF5555)));
            player.sendMessage(Component.text("You can use a player name or UUID")
                    .color(TextColor.color(0xAAAAAA)));
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null) {
            player.sendMessage(Component.text("You must be looking at a pedestal!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        Location loc = targetBlock.getLocation();
        UUID currentOwner = ownerStorage.getOwner(loc);

        if (currentOwner == null) {
            player.sendMessage(Component.text("This pedestal has no owner!")
                    .color(TextColor.color(0xFF5555)));
            player.sendMessage(Component.text("Use /pedestal claim to claim it first")
                    .color(TextColor.color(0xFFAA00)));
            return;
        }

        // CRITICAL: Check permissions properly
        // Only allow transfer if player is owner OR has admin permission
        // The bypass permission should NOT allow transfers
        boolean isOwner = currentOwner.equals(player.getUniqueId());
        boolean hasAdmin = player.hasPermission("elementsmp.pedestal.admin");

        if (!isOwner && !hasAdmin) {
            player.sendMessage(Component.text("You don't own this pedestal and don't have admin permission!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        // Try to parse as UUID first, then as player name
        UUID newOwnerUUID = null;
        String newOwnerName = args[1];

        // Try parsing as UUID
        try {
            newOwnerUUID = UUID.fromString(args[1]);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try as player name
            @SuppressWarnings("deprecation")
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);

            // Check if the player has ever played (or is online)
            if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
                newOwnerUUID = offlinePlayer.getUniqueId();
                newOwnerName = offlinePlayer.getName();
            } else {
                player.sendMessage(Component.text("Player '" + args[1] + "' not found!")
                        .color(TextColor.color(0xFF5555)));
                player.sendMessage(Component.text("Use a valid player name or UUID")
                        .color(TextColor.color(0xAAAAAA)));
                return;
            }
        }

        // FIXED: Check if transferring to the CURRENT owner, not the player executing the command
        if (newOwnerUUID.equals(currentOwner)) {
            String currentOwnerName = Bukkit.getOfflinePlayer(currentOwner).getName();
            player.sendMessage(Component.text("This pedestal is already owned by " +
                            (currentOwnerName != null ? currentOwnerName : currentOwner.toString()) + "!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        // Set the new owner
        ownerStorage.setOwner(loc, newOwnerUUID);

        // Get the actual name for display
        OfflinePlayer newOwnerPlayer = Bukkit.getOfflinePlayer(newOwnerUUID);
        String displayName = newOwnerPlayer.getName() != null ? newOwnerPlayer.getName() : newOwnerUUID.toString();

        player.sendMessage(Component.text("Pedestal transferred to " + displayName)
                .color(TextColor.color(0x55FF55)));

        if (!newOwnerPlayer.isOnline()) {
            player.sendMessage(Component.text("Note: Player is currently offline")
                    .color(TextColor.color(0xFFAA00)));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("=== Pedestal Admin Commands ===")
                .color(TextColor.color(0x55FFFF)));
        sender.sendMessage(Component.text("/pedestal info - View storage information")
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("/pedestal reload - Reload from YAML backup")
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("/pedestal check - Check pedestal owner")
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("/pedestal claim - Claim an unclaimed pedestal")
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("/pedestal transfer <player|uuid> - Transfer ownership")
                .color(TextColor.color(0xFFFFFF)));
        sender.sendMessage(Component.text("  Example: /pedestal transfer Notch")
                .color(TextColor.color(0xAAAAAA)));
        sender.sendMessage(Component.text("  Example: /pedestal transfer 069a79f4-44e9-4726-a5be-fca90e38aaf5")
                .color(TextColor.color(0xAAAAAA)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("info");
            completions.add("reload");
            completions.add("check");
            completions.add("claim");
            completions.add("transfer");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("transfer")) {
            // Add online player names
            Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));

            // Add some helpful suggestions
            completions.add("<player_name>");
            completions.add("<uuid>");
        }

        return completions;
    }
}