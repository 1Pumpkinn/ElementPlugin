package saturn.elementPlugin.commands;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.TrustManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enhanced trust command with team support
 */
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
            sendUsage(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> handleList(p);
            case "add" -> handleAdd(p, args);
            case "remove" -> handleRemove(p, args);
            case "accept" -> handleAccept(p, args);
            case "deny" -> handleDeny(p, args);
            case "team" -> handleTeam(p, args);
            default -> sendUsage(p);
        }

        return true;
    }

    private void sendUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=== Trust & Team Commands ===");
        p.sendMessage(ChatColor.YELLOW + "/trust list " + ChatColor.GRAY + "- View trusted players and team");
        p.sendMessage(ChatColor.YELLOW + "/trust add <player> " + ChatColor.GRAY + "- Send trust request");
        p.sendMessage(ChatColor.YELLOW + "/trust remove <player> " + ChatColor.GRAY + "- Remove trust");
        p.sendMessage(ChatColor.YELLOW + "/trust accept <player> " + ChatColor.GRAY + "- Accept trust request");
        p.sendMessage(ChatColor.YELLOW + "/trust deny <player> " + ChatColor.GRAY + "- Deny trust request");
        p.sendMessage(ChatColor.YELLOW + "/trust team " + ChatColor.GRAY + "- Team management");
    }

    private void handleList(Player p) {
        // Show team info
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName != null) {
            p.sendMessage(ChatColor.AQUA + "Team: " + ChatColor.WHITE + teamName);

            boolean isLeader = trust.isTeamLeader(p.getUniqueId(), teamName);
            if (isLeader) {
                p.sendMessage(ChatColor.GOLD + "  Role: Team Leader");
            }

            p.sendMessage(ChatColor.AQUA + "Members:");
            for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
                String memberName = member.getName() != null ? member.getName() : "Unknown";
                boolean online = member.isOnline();
                String status = online ? ChatColor.GREEN + "●" : ChatColor.GRAY + "●";

                if (memberUUID.equals(p.getUniqueId())) {
                    p.sendMessage("  " + status + " " + ChatColor.WHITE + memberName + ChatColor.YELLOW + " (You)");
                } else if (trust.isTeamLeader(memberUUID, teamName)) {
                    p.sendMessage("  " + status + " " + ChatColor.WHITE + memberName + ChatColor.GOLD + " (Leader)");
                } else {
                    p.sendMessage("  " + status + " " + ChatColor.WHITE + memberName);
                }
            }
            p.sendMessage("");
        }

        // Show individual trust
        var names = trust.getTrustedNames(p.getUniqueId());
        List<String> displayNames = new ArrayList<>();
        List<String> unknownUUIDs = new ArrayList<>();

        for (String name : names) {
            try {
                UUID uuid = UUID.fromString(name);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                String playerName = offlinePlayer.getName();
                if (playerName != null) {
                    displayNames.add(playerName);
                } else {
                    unknownUUIDs.add(uuid.toString().substring(0, 8) + "...");
                }
            } catch (IllegalArgumentException e) {
                displayNames.add(name);
            }
        }

        if (displayNames.isEmpty() && unknownUUIDs.isEmpty()) {
            p.sendMessage(ChatColor.AQUA + "Individual Trust: " + ChatColor.WHITE + "(none)");
        } else {
            p.sendMessage(ChatColor.AQUA + "Individual Trust: " + ChatColor.WHITE + String.join(", ", displayNames));
            if (!unknownUUIDs.isEmpty()) {
                p.sendMessage(ChatColor.GRAY + "Unknown players: " + String.join(", ", unknownUUIDs));
            }
        }
    }

    private void handleAdd(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /trust add <player>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found");
            return;
        }

        if (target.equals(p)) {
            p.sendMessage(ChatColor.RED + "You cannot trust yourself");
            return;
        }

        if (trust.isTrusted(p.getUniqueId(), target.getUniqueId()) &&
                trust.isTrusted(target.getUniqueId(), p.getUniqueId())) {
            p.sendMessage(ChatColor.YELLOW + "You are already mutually trusted.");
            return;
        }

        trust.addPending(target.getUniqueId(), p.getUniqueId());

        Component msg = Component.text(p.getName() + " wants to trust with you. ", NamedTextColor.GOLD)
                .append(Component.text("[ACCEPT]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/trust accept " + p.getUniqueId())))
                .append(Component.text(" "))
                .append(Component.text("[DENY]", NamedTextColor.RED)
                        .clickEvent(ClickEvent.runCommand("/trust deny " + p.getUniqueId())));
        target.sendMessage(msg);
        p.sendMessage(ChatColor.GREEN + "Sent trust request to " + target.getName());
    }

    private void handleRemove(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /trust remove <player>");
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
                p.sendMessage(ChatColor.RED + "Player must be online or provide UUID");
                return;
            }
        }

        trust.removeMutualTrust(p.getUniqueId(), uuid);
        p.sendMessage(ChatColor.YELLOW + "Removed mutual trust.");
    }

    private void handleAccept(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /trust accept <player|uuid>");
            return;
        }

        Player from = Bukkit.getPlayer(args[1]);
        UUID fromId = null;
        if (from != null) {
            fromId = from.getUniqueId();
        } else {
            try {
                fromId = UUID.fromString(args[1]);
            } catch (Exception ex) {
                p.sendMessage(ChatColor.RED + "Player not found");
                return;
            }
        }

        if (!trust.hasPending(p.getUniqueId(), fromId)) {
            p.sendMessage(ChatColor.YELLOW + "No pending request from that player.");
            return;
        }

        trust.clearPending(p.getUniqueId(), fromId);
        trust.addMutualTrust(p.getUniqueId(), fromId);
        p.sendMessage(ChatColor.GREEN + "You are now mutually trusted.");

        Player other = Bukkit.getPlayer(fromId);
        if (other != null) {
            other.sendMessage(ChatColor.GREEN + p.getName() + " accepted your trust request.");
        }
    }

    private void handleDeny(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(ChatColor.RED + "Usage: /trust deny <player|uuid>");
            return;
        }

        Player from = Bukkit.getPlayer(args[1]);
        UUID fromId = null;
        if (from != null) {
            fromId = from.getUniqueId();
        } else {
            try {
                fromId = UUID.fromString(args[1]);
            } catch (Exception ex) {
                p.sendMessage(ChatColor.RED + "Player not found");
                return;
            }
        }

        if (trust.hasPending(p.getUniqueId(), fromId)) {
            trust.clearPending(p.getUniqueId(), fromId);
            p.sendMessage(ChatColor.YELLOW + "Denied trust request.");
            Player other = Bukkit.getPlayer(fromId);
            if (other != null) {
                other.sendMessage(ChatColor.RED + p.getName() + " denied your trust request.");
            }
        } else {
            p.sendMessage(ChatColor.YELLOW + "No pending request from that player.");
        }
    }

    private void handleTeam(Player p, String[] args) {
        if (args.length < 2) {
            sendTeamUsage(p);
            return;
        }

        switch (args[1].toLowerCase()) {
            case "create" -> handleTeamCreate(p, args);
            case "invite" -> handleTeamInvite(p, args);
            case "accept" -> handleTeamAccept(p, args);
            case "leave" -> handleTeamLeave(p);
            case "kick" -> handleTeamKick(p, args);
            case "disband" -> handleTeamDisband(p);
            default -> sendTeamUsage(p);
        }
    }

    private void sendTeamUsage(Player p) {
        p.sendMessage(ChatColor.GOLD + "=== Team Commands ===");
        p.sendMessage(ChatColor.YELLOW + "/trust team create <name> " + ChatColor.GRAY + "- Create a team");
        p.sendMessage(ChatColor.YELLOW + "/trust team invite <player> " + ChatColor.GRAY + "- Invite to your team");
        p.sendMessage(ChatColor.YELLOW + "/trust team accept <name> " + ChatColor.GRAY + "- Accept team invite");
        p.sendMessage(ChatColor.YELLOW + "/trust team leave " + ChatColor.GRAY + "- Leave your team");
        p.sendMessage(ChatColor.YELLOW + "/trust team kick <player> " + ChatColor.GRAY + "- Kick from team (leader)");
        p.sendMessage(ChatColor.YELLOW + "/trust team disband " + ChatColor.GRAY + "- Disband team (leader)");
    }

    private void handleTeamCreate(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /trust team create <name>");
            return;
        }

        String teamName = args[2];
        if (trust.createTeam(p, teamName)) {
            p.sendMessage(ChatColor.GREEN + "Created team: " + ChatColor.AQUA + teamName);
            p.sendMessage(ChatColor.GRAY + "Use /trust team invite <player> to add members");
        } else {
            p.sendMessage(ChatColor.RED + "Failed to create team. You may already be in a team, or the name is taken/invalid.");
        }
    }

    private void handleTeamInvite(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /trust team invite <player>");
            return;
        }

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (trust.inviteToTeam(p, target, teamName)) {
            Component msg = Component.text(p.getName() + " invited you to team ", NamedTextColor.GOLD)
                    .append(Component.text(teamName, NamedTextColor.AQUA))
                    .append(Component.text(". ", NamedTextColor.GOLD))
                    .append(Component.text("[ACCEPT]", NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/trust team accept " + teamName)));
            target.sendMessage(msg);
            p.sendMessage(ChatColor.GREEN + "Sent team invite to " + target.getName());
        } else {
            p.sendMessage(ChatColor.RED + "Failed to invite. You must be team leader, and they must not be in another team.");
        }
    }

    private void handleTeamAccept(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /trust team accept <team_name>");
            return;
        }

        String teamName = args[2];
        if (trust.acceptTeamInvite(p, teamName)) {
            p.sendMessage(ChatColor.GREEN + "Joined team: " + ChatColor.AQUA + teamName);

            // Notify team members
            for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(p)) {
                    member.sendMessage(ChatColor.AQUA + p.getName() + ChatColor.GREEN + " joined the team!");
                }
            }
        } else {
            p.sendMessage(ChatColor.RED + "Failed to join team. No pending invite or invalid team.");
        }
    }

    private void handleTeamLeave(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        boolean isLeader = trust.isTeamLeader(p.getUniqueId(), teamName);

        if (trust.leaveTeam(p)) {
            if (isLeader) {
                p.sendMessage(ChatColor.YELLOW + "Team disbanded (you were the leader).");
            } else {
                p.sendMessage(ChatColor.YELLOW + "Left team: " + teamName);

                // Notify remaining members
                for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null) {
                        member.sendMessage(ChatColor.YELLOW + p.getName() + " left the team.");
                    }
                }
            }
        } else {
            p.sendMessage(ChatColor.RED + "Failed to leave team.");
        }
    }

    private void handleTeamKick(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(ChatColor.RED + "Usage: /trust team kick <player>");
            return;
        }

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            p.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (trust.kickFromTeam(p, target, teamName)) {
            p.sendMessage(ChatColor.GREEN + "Kicked " + target.getName() + " from the team.");
            target.sendMessage(ChatColor.RED + "You were kicked from team: " + teamName);
        } else {
            p.sendMessage(ChatColor.RED + "Failed to kick. You must be team leader.");
        }
    }

    private void handleTeamDisband(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(ChatColor.RED + "You are not in a team.");
            return;
        }

        if (!trust.isTeamLeader(p.getUniqueId(), teamName)) {
            p.sendMessage(ChatColor.RED + "Only the team leader can disband the team.");
            return;
        }

        // Notify all members
        for (UUID memberUUID : trust.getTeamMembers(teamName)) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(ChatColor.RED + "Team " + teamName + " has been disbanded.");
            }
        }

        trust.disbandTeam(teamName);
        p.sendMessage(ChatColor.YELLOW + "Team disbanded.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("list", "add", "remove", "accept", "deny", "team");
            return subcommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            if ("team".equals(subcommand)) {
                List<String> teamCommands = Arrays.asList("create", "invite", "accept", "leave", "kick", "disband");
                return teamCommands.stream()
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            switch (subcommand) {
                case "add" -> {
                    return getOnlinePlayers(args[1]);
                }
                case "remove" -> {
                    return getTrustedPlayers(p, args[1]);
                }
                case "accept", "deny" -> {
                    return getPendingPlayers(p, args[1]);
                }
            }
        }

        if (args.length == 3 && "team".equals(args[0].toLowerCase())) {
            if ("invite".equals(args[1].toLowerCase()) || "kick".equals(args[1].toLowerCase())) {
                return getOnlinePlayers(args[2]);
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

    private List<String> getTrustedPlayers(Player p, String prefix) {
        List<String> names = trust.getTrustedNames(p.getUniqueId());
        return names.stream()
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> getPendingPlayers(Player p, String prefix) {
        List<String> suggestions = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (trust.hasPending(p.getUniqueId(), online.getUniqueId())) {
                if (online.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                    suggestions.add(online.getName());
                }
            }
        }
        return suggestions;
    }
}