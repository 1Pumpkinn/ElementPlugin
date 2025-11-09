package hs.elementPlugin.commands;

import hs.elementPlugin.managers.TrustManager;
import hs.elementPlugin.ElementPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrustCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final TrustManager trust;

    public TrustCommand(ElementPlugin plugin, TrustManager trust) {
        this.plugin = plugin;
        this.trust = trust;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /trust <list|add|remove> [player]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> {
                var names = trust.getTrustedNames(p.getUniqueId());

                // If getTrustedNames returns UUIDs (from old implementation), convert them
                List<String> displayNames = new ArrayList<>();
                List<String> unknownUUIDs = new ArrayList<>();

                for (String name : names) {
                    try {
                        // Try to parse as UUID
                        UUID uuid = UUID.fromString(name);
                        // Convert UUID to player name
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                        String playerName = offlinePlayer.getName();
                        if (playerName != null) {
                            displayNames.add(playerName);
                        } else {
                            // If name is null, this player never joined the server
                            unknownUUIDs.add(uuid.toString().substring(0, 8) + "...");
                        }
                    } catch (IllegalArgumentException e) {
                        // Not a UUID, assume it's already a name
                        displayNames.add(name);
                    }
                }

                if (displayNames.isEmpty() && unknownUUIDs.isEmpty()) {
                    p.sendMessage(ChatColor.AQUA + "Trusted: " + ChatColor.WHITE + "(none)");
                } else {
                    p.sendMessage(ChatColor.AQUA + "Trusted: " + ChatColor.WHITE + String.join(", ", displayNames));
                    if (!unknownUUIDs.isEmpty()) {
                        p.sendMessage(ChatColor.GRAY + "Unknown players: " + String.join(", ", unknownUUIDs));
                        p.sendMessage(ChatColor.GRAY + "Use '/trust remove <uuid>' to clean up invalid entries");
                    }
                }
            }
            case "add" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust add <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                if (target.equals(p)) { p.sendMessage(ChatColor.RED + "You cannot trust yourself"); return true; }
                if (trust.isTrusted(p.getUniqueId(), target.getUniqueId()) && trust.isTrusted(target.getUniqueId(), p.getUniqueId())) {
                    p.sendMessage(ChatColor.YELLOW + "You are already mutually trusted.");
                    return true;
                }
                trust.addPending(target.getUniqueId(), p.getUniqueId());
                // Send clickable message to target
                Component msg = Component.text(p.getName() + " wants to trust with you. ", NamedTextColor.GOLD)
                        .append(Component.text("[ACCEPT]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/trust accept " + p.getUniqueId())))
                        .append(Component.text(" "))
                        .append(Component.text("[DENY]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/trust deny " + p.getUniqueId())));
                target.sendMessage(msg);
                p.sendMessage(ChatColor.GREEN + "Sent trust request to " + target.getName());
            }
            case "accept" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust accept <player|uuid>"); return true; }
                Player from = Bukkit.getPlayer(args[1]);
                UUID fromId = null;
                if (from != null) fromId = from.getUniqueId();
                else {
                    try { fromId = UUID.fromString(args[1]); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                }
                if (!trust.hasPending(p.getUniqueId(), fromId)) { p.sendMessage(ChatColor.YELLOW + "No pending request from that player."); return true; }
                trust.clearPending(p.getUniqueId(), fromId);
                trust.addMutualTrust(p.getUniqueId(), fromId);
                p.sendMessage(ChatColor.GREEN + "You are now mutually trusted.");
                Player other = Bukkit.getPlayer(fromId);
                if (other != null) other.sendMessage(ChatColor.GREEN + p.getName() + " accepted your trust request.");
            }
            case "deny" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust deny <player|uuid>"); return true; }
                Player from = Bukkit.getPlayer(args[1]);
                UUID fromId = null;
                if (from != null) fromId = from.getUniqueId();
                else {
                    try { fromId = UUID.fromString(args[1]); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                }
                if (trust.hasPending(p.getUniqueId(), fromId)) {
                    trust.clearPending(p.getUniqueId(), fromId);
                    p.sendMessage(ChatColor.YELLOW + "Denied trust request.");
                    Player other = Bukkit.getPlayer(fromId);
                    if (other != null) other.sendMessage(ChatColor.RED + p.getName() + " denied your trust request.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "No pending request from that player.");
                }
            }
            case "remove" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust remove <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                UUID uuid;
                if (target != null) uuid = target.getUniqueId(); else {
                    // Fallback: try parsing UUID
                    try { uuid = UUID.fromString(args[1]); } catch (IllegalArgumentException ex) {
                        p.sendMessage(ChatColor.RED + "Player must be online or provide UUID");
                        return true;
                    }
                }
                trust.removeMutualTrust(p.getUniqueId(), uuid);
                p.sendMessage(ChatColor.YELLOW + "Removed mutual trust.");
            }
            default -> p.sendMessage(ChatColor.YELLOW + "Usage: /trust <list|add|remove> [player]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            return new ArrayList<>();
        }

        // First argument: show subcommands
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("list", "add", "remove", "accept", "deny");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Second argument: show player names based on subcommand
        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();

            switch (subcommand) {
                case "add" -> {
                    // Show online players except self and already trusted players
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(p) &&
                                !trust.isTrusted(p.getUniqueId(), online.getUniqueId())) {
                            suggestions.add(online.getName());
                        }
                    }
                }
                case "remove" -> {
                    // Show currently trusted players (convert UUIDs to names)
                    var names = trust.getTrustedNames(p.getUniqueId());
                    for (String name : names) {
                        try {
                            UUID uuid = UUID.fromString(name);
                            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                            String playerName = offlinePlayer.getName();
                            if (playerName != null) {
                                suggestions.add(playerName);
                            }
                        } catch (IllegalArgumentException e) {
                            suggestions.add(name);
                        }
                    }
                }
                case "accept", "deny" -> {
                    // Show players who have sent pending requests
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.equals(p) && trust.hasPending(p.getUniqueId(), online.getUniqueId())) {
                            suggestions.add(online.getName());
                        }
                    }
                }
            }

            return suggestions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}