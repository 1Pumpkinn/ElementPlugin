package saturn.elementPlugin.commands;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.TrustManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Command handler for the trust system
 * /trust <player> - Send a trust request
 * /trust accept <player> - Accept a trust request
 * /trust deny <player> - Deny a trust request
 * /trust remove <player> - Remove trust
 * /trust list - List trusted players
 * /trust requests - List pending requests
 */
public class TrustCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final TrustManager trustManager;

    public TrustCommand(ElementPlugin plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String arg0 = args[0].toLowerCase();
            switch (arg0) {
                case "add":
                    if (args.length >= 2) {
                        handleTrustRequest(player, args[1]);
                    } else {
                        player.sendMessage(ChatColor.RED + "Usage: /trust add <player>");
                    }
                    break;
                case "remove":
                case "untrust":
                    handleRemove(player, args);
                    break;
                case "accept":
                    handleAccept(player, args);
                    break;
                case "deny":
                    handleDeny(player, args);
                    break;
                case "list":
                    handleList(player);
                    break;
                case "requests":
                    handleRequests(player);
                    break;
                case "help":
                    sendHelp(player);
                    break;
                default:
                    // If it's just a player name, treat as add
                    handleTrustRequest(player, args[0]);
                    break;
            }

        return true;
    }

    /**
     * Send a trust request to another player
     */
    private void handleTrustRequest(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + targetName + "' not found!");
            return;
        }

        if (target.equals(sender)) {
            sender.sendMessage(ChatColor.RED + "You cannot trust yourself!");
            return;
        }

        // Check if already trusted
        if (trustManager.trusts(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(ChatColor.RED + "You already trust " + target.getName() + "!");
            return;
        }

        // Send trust request
        if (trustManager.sendTrustRequest(sender.getUniqueId(), target.getUniqueId())) {
            sender.sendMessage(ChatColor.GREEN + "Trust request sent to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + "!");

            // Send notification to target with clickable accept/deny buttons
            sendTrustRequestNotification(target, sender);
        } else {
            sender.sendMessage(ChatColor.RED + "You already have a pending request to " + target.getName() + "!");
        }
    }

    /**
     * Send a clickable trust request notification to a player
     */
    private void sendTrustRequestNotification(Player target, Player sender) {
        Component message = Component.empty()
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.text("Trust Request").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text(sender.getName()).color(NamedTextColor.AQUA))
                .append(Component.text(" wants you to trust them!").color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("This will prevent their abilities from affecting you.").color(NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("[ACCEPT]").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/trust accept " + sender.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to accept").color(NamedTextColor.GREEN))))
                .append(Component.text("  "))
                .append(Component.text("[DENY]").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/trust deny " + sender.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to deny").color(NamedTextColor.RED))))
                .append(Component.newline())
                .append(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━").color(NamedTextColor.DARK_GRAY));

        target.sendMessage(message);
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
    }

    /**
     * Accept a trust request
     */
    private void handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /trust accept <player>");
            return;
        }

        Player requester = Bukkit.getPlayer(args[1]);

        if (requester == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found!");
            return;
        }

        if (trustManager.acceptTrustRequest(player.getUniqueId(), requester.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "You now trust " + ChatColor.AQUA + requester.getName() + ChatColor.GREEN + "!");

            requester.sendMessage(ChatColor.GREEN + "" + ChatColor.AQUA + player.getName() + ChatColor.GREEN + " accepted your trust request!");

            // Check if mutual trust
            if (trustManager.mutualTrust(player.getUniqueId(), requester.getUniqueId())) {
                player.sendMessage(ChatColor.GOLD + "✦ You and " + requester.getName() + " now mutually trust each other!");
                requester.sendMessage(ChatColor.GOLD + "✦ You and " + player.getName() + " now mutually trust each other!");
            }
        } else {
            player.sendMessage(ChatColor.RED + "No pending trust request from " + requester.getName() + "!");
        }
    }

    /**
     * Deny a trust request
     */
    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /trust deny <player>");
            return;
        }

        Player requester = Bukkit.getPlayer(args[1]);

        if (requester == null) {
            player.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found!");
            return;
        }

        if (trustManager.denyTrustRequest(player.getUniqueId(), requester.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You denied the trust request from " + requester.getName());
            requester.sendMessage(ChatColor.YELLOW + player.getName() + " denied your trust request.");
        } else {
            player.sendMessage(ChatColor.RED + "No pending trust request from " + requester.getName() + "!");
        }
    }

    /**
     * Remove trust from a player
     */
    private void handleRemove(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /trust remove <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID targetUuid;
        String targetName;

        if (target != null) {
            targetUuid = target.getUniqueId();
            targetName = target.getName();
        } else {
            // Try to find by name in trusted list
            UUID found = null;
            String foundName = args[1];

            for (UUID uuid : trustManager.getTrustedPlayers(player.getUniqueId())) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.getName().equalsIgnoreCase(args[1])) {
                    found = uuid;
                    foundName = p.getName();
                    break;
                }
            }

            if (found == null) {
                player.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found in your trust list!");
                return;
            }

            targetUuid = found;
            targetName = foundName;
        }

        // Check if player trusts them
        if (!trustManager.trusts(player.getUniqueId(), targetUuid)) {
            player.sendMessage(ChatColor.RED + "You don't trust " + targetName + "!");
            return;
        }

        trustManager.removeTrust(player.getUniqueId(), targetUuid);
        player.sendMessage(ChatColor.YELLOW + "You no longer trust " + ChatColor.AQUA + targetName);

        // Notify target if online
        if (target != null) {
            target.sendMessage(ChatColor.YELLOW + player.getName() + " no longer trusts you.");
        }
    }

    /**
     * List all trusted players
     */
    private void handleList(Player player) {
        Set<UUID> trusted = trustManager.getTrustedPlayers(player.getUniqueId());

        if (trusted.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You don't trust anyone yet.");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/trust <player>" + ChatColor.GRAY + " to send a trust request.");
            return;
        }

        player.sendMessage(Component.text("━━━ Trusted Players ━━━").color(NamedTextColor.GOLD));

        for (UUID uuid : trusted) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString();
            boolean online = p != null;
            boolean mutual = trustManager.trusts(uuid, player.getUniqueId());

            Component line = Component.text("• ").color(NamedTextColor.GRAY)
                    .append(Component.text(name).color(online ? NamedTextColor.AQUA : NamedTextColor.DARK_GRAY));

            if (mutual) {
                line = line.append(Component.text(" ✦").color(NamedTextColor.GOLD)
                        .hoverEvent(HoverEvent.showText(Component.text("Mutual Trust").color(NamedTextColor.GOLD))));
            } else {
                line = line.append(Component.text(" (One-way)").color(NamedTextColor.YELLOW)
                        .hoverEvent(HoverEvent.showText(Component.text("You trust them, but not vice versa.").color(NamedTextColor.YELLOW))));
            }

            if (!online) {
                line = line.append(Component.text(" (Offline)").color(NamedTextColor.DARK_GRAY));
            }

            line = line.append(Component.text(" [REMOVE]").color(NamedTextColor.RED)
                .clickEvent(ClickEvent.runCommand("/trust remove " + name))
                .hoverEvent(HoverEvent.showText(Component.text("Remove trust for " + name).color(NamedTextColor.RED))));

            player.sendMessage(line);
        }

        player.sendMessage(Component.text("Total: " + trusted.size() + " player(s)").color(NamedTextColor.GRAY));
    }

    /**
     * List pending trust requests
     */
    private void handleRequests(Player player) {
        Set<UUID> pending = trustManager.getPendingRequests(player.getUniqueId());

        if (pending.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no pending trust requests.");
            return;
        }

        player.sendMessage(Component.text("━━━ Pending Trust Requests ━━━").color(NamedTextColor.GOLD));

        for (UUID uuid : pending) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : uuid.toString();

            Component line = Component.text("• ").color(NamedTextColor.GRAY)
                    .append(Component.text(name).color(NamedTextColor.AQUA))
                    .append(Component.text("  "))
                    .append(Component.text("[ACCEPT]").color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/trust accept " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Accept trust request from " + name).color(NamedTextColor.GREEN))))
                    .append(Component.text(" "))
                    .append(Component.text("[DENY]").color(NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/trust deny " + name))
                            .hoverEvent(HoverEvent.showText(Component.text("Deny trust request from " + name).color(NamedTextColor.RED))));

            player.sendMessage(line);
        }

        player.sendMessage(Component.text("Total: " + pending.size() + " request(s)").color(NamedTextColor.GRAY));
    }

    /**
     * Send help message
     */
    private void sendHelp(Player player) {
        player.sendMessage(Component.text("━━━ Trust System Commands ━━━").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/trust add <player>").color(NamedTextColor.AQUA)
                .append(Component.text(" - Send a trust request").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust accept <player>").color(NamedTextColor.AQUA)
                .append(Component.text(" - Accept a trust request").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust deny <player>").color(NamedTextColor.AQUA)
                .append(Component.text(" - Deny a trust request").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust remove <player>").color(NamedTextColor.AQUA)
                .append(Component.text(" - Remove trust from a player").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust list").color(NamedTextColor.AQUA)
                .append(Component.text(" - List trusted players").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust requests").color(NamedTextColor.AQUA)
                .append(Component.text(" - List pending requests").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("/trust help").color(NamedTextColor.AQUA)
                .append(Component.text(" - Show this help message").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Aliases: /trust untrust <player> | /trust <player>").color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("accept", "deny", "remove", "list", "requests"));

            // Add online player names
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.equals(player)) {
                    completions.add(p.getName());
                }
            }

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("accept") || subcommand.equals("deny")) {
                // Show players with pending requests
                Set<UUID> pending = trustManager.getPendingRequests(player.getUniqueId());
                return pending.stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (subcommand.equals("remove")) {
                // Show trusted players
                Set<UUID> trusted = trustManager.getTrustedPlayers(player.getUniqueId());
                return trusted.stream()
                        .map(Bukkit::getPlayer)
                        .filter(Objects::nonNull)
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}