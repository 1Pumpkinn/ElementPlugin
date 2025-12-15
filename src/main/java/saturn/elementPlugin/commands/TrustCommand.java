package saturn.elementPlugin.commands;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simplified trust command with team customization
 * Individual trust uses /trust, teams use /team
 */
public class TrustCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final TeamManager trust;

    public TrustCommand(ElementPlugin plugin, TeamManager trust) {
        this.plugin = plugin;
        this.trust = trust;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only");
            return true;
        }

        if (args.length == 0) {
            handleList(p);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleList(p);
            case "accept" -> handleAccept(p, args);
            case "deny", "decline" -> handleDeny(p, args);
            case "remove", "untrust" -> handleRemove(p, args);
            case "help" -> sendUsage(p);
            default -> handleQuickTrust(p, args[0]);
        }

        return true;
    }

    private void handleQuickTrust(Player p, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            p.sendMessage(Component.text("Player not found or offline", NamedTextColor.RED));
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(Component.text("You cannot trust yourself", NamedTextColor.RED));
            return;
        }

        if (trust.isTrusted(p.getUniqueId(), target.getUniqueId())) {
            p.sendMessage(Component.text("You are already trusted with " + target.getName(), NamedTextColor.YELLOW));
            return;
        }

        trust.addPending(target.getUniqueId(), p.getUniqueId());

        Component msg = Component.text(p.getName() + " wants to trust with you. ", NamedTextColor.GOLD)
                .append(Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/trust accept " + p.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to accept"))))
                .append(Component.text(" "))
                .append(Component.text("[DENY]", NamedTextColor.RED, TextDecoration.BOLD)
                        .clickEvent(ClickEvent.runCommand("/trust deny " + p.getName()))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))));
        target.sendMessage(msg);

        p.sendMessage(Component.text("✓ Sent trust request to " + target.getName(), NamedTextColor.GREEN));
    }

    private void handleList(Player p) {
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName != null) {
            boolean isLeader = trust.isTeamLeader(p.getUniqueId(), teamName);

            p.sendMessage(Component.text("⚔ Team: ", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .append(Component.text(teamName, NamedTextColor.AQUA)));

            if (isLeader) {
                p.sendMessage(Component.text("  Role: ", NamedTextColor.YELLOW)
                        .append(Component.text("Leader ⭐", NamedTextColor.GOLD)));
            }

            p.sendMessage(Component.text("  Members:", NamedTextColor.YELLOW));
            for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
                String memberName = member.getName() != null ? member.getName() : "Unknown";
                boolean online = member.isOnline();

                Component statusIcon = online
                        ? Component.text("●", NamedTextColor.GREEN)
                        : Component.text("●", NamedTextColor.GRAY);

                Component nameComponent = Component.text("    ")
                        .append(statusIcon)
                        .append(Component.text(" " + memberName, NamedTextColor.WHITE));

                if (memberUUID.equals(p.getUniqueId())) {
                    nameComponent = nameComponent.append(Component.text(" (You)", NamedTextColor.YELLOW));
                } else if (trust.isTeamLeader(memberUUID, teamName)) {
                    nameComponent = nameComponent.append(Component.text(" ⭐", NamedTextColor.GOLD));
                }

                p.sendMessage(nameComponent);
            }
            p.sendMessage(Component.empty());
        }

        var names = trust.getTrustedNames(p.getUniqueId());
        List<String> displayNames = new ArrayList<>();

        for (String name : names) {
            try {
                UUID uuid = UUID.fromString(name);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName();
                if (playerName != null) {
                    displayNames.add(playerName);
                }
            } catch (IllegalArgumentException e) {
                displayNames.add(name);
            }
        }

        if (displayNames.isEmpty()) {
            p.sendMessage(Component.text("❤ Individual Trust: ", NamedTextColor.RED)
                    .append(Component.text("(none)", NamedTextColor.GRAY)));
        } else {
            p.sendMessage(Component.text("❤ Individual Trust: ", NamedTextColor.RED)
                    .append(Component.text(String.join(", ", displayNames), NamedTextColor.WHITE)));
        }

        var pendingInvites = trust.getPendingInvites(p.getUniqueId());
        if (!pendingInvites.isEmpty()) {
            p.sendMessage(Component.empty());
            p.sendMessage(Component.text("📨 Pending Team Invites:", NamedTextColor.YELLOW));
            for (String invite : pendingInvites) {
                Component inviteMsg = Component.text("  • " + invite + " ", NamedTextColor.WHITE)
                        .append(Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/team accept " + invite))
                                .hoverEvent(HoverEvent.showText(Component.text("Click to join " + invite))));
                p.sendMessage(inviteMsg);
            }
        }

        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        p.sendMessage(Component.text("Tip: Use /trust <player> to send trust request", NamedTextColor.GRAY));
        p.sendMessage(Component.text("Use /team for team commands", NamedTextColor.GRAY));
    }

    private void handleAccept(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /trust accept <player>", NamedTextColor.RED));
            return;
        }

        Player from = Bukkit.getPlayer(args[1]);
        UUID fromId = from != null ? from.getUniqueId() : null;

        if (fromId == null) {
            p.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        if (!trust.hasPending(p.getUniqueId(), fromId)) {
            p.sendMessage(Component.text("No pending request from that player", NamedTextColor.YELLOW));
            return;
        }

        trust.clearPending(p.getUniqueId(), fromId);
        trust.addMutualTrust(p.getUniqueId(), fromId);

        p.sendMessage(Component.text("✓ You are now mutually trusted with " + args[1], NamedTextColor.GREEN));

        Player other = Bukkit.getPlayer(fromId);
        if (other != null) {
            other.sendMessage(Component.text("✓ " + p.getName() + " accepted your trust request", NamedTextColor.GREEN));
        }
    }

    private void handleDeny(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /trust deny <player>", NamedTextColor.RED));
            return;
        }

        Player from = Bukkit.getPlayer(args[1]);
        UUID fromId = from != null ? from.getUniqueId() : null;

        if (fromId == null) {
            p.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        if (trust.hasPending(p.getUniqueId(), fromId)) {
            trust.clearPending(p.getUniqueId(), fromId);
            p.sendMessage(Component.text("✓ Denied trust request", NamedTextColor.YELLOW));

            Player other = Bukkit.getPlayer(fromId);
            if (other != null) {
                other.sendMessage(Component.text(p.getName() + " denied your trust request", NamedTextColor.RED));
            }
        } else {
            p.sendMessage(Component.text("No pending request from that player", NamedTextColor.YELLOW));
        }
    }

    private void handleRemove(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /trust remove <player>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        UUID uuid;

        if (target != null) {
            uuid = target.getUniqueId();
        } else {
            try {
                uuid = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ex) {
                p.sendMessage(Component.text("Player must be online or provide UUID", NamedTextColor.RED));
                return;
            }
        }

        trust.removeMutualTrust(p.getUniqueId(), uuid);
        p.sendMessage(Component.text("✓ Removed mutual trust", NamedTextColor.YELLOW));
    }

    private void sendUsage(Player p) {
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        p.sendMessage(Component.text("Trust Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("Individual Trust:", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("  /trust ", NamedTextColor.AQUA)
                .append(Component.text("- View your trust status", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /trust <player> ", NamedTextColor.AQUA)
                .append(Component.text("- Send trust request", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /trust accept <player> ", NamedTextColor.AQUA)
                .append(Component.text("- Accept trust request", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /trust remove <player> ", NamedTextColor.AQUA)
                .append(Component.text("- Remove trust", NamedTextColor.GRAY)));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("Teams:", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("  Use /team for all team commands", NamedTextColor.AQUA));
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("list", "accept", "deny", "remove", "help");
            List<String> completions = new ArrayList<>(subcommands);
            completions.addAll(getOnlinePlayers(args[0]));
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if ("accept".equals(subcommand) || "deny".equals(subcommand) || "remove".equals(subcommand)) {
                return getOnlinePlayers(args[1]);
            }
        }

        return new ArrayList<>();
    }

    private List<String> getOnlinePlayers(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}