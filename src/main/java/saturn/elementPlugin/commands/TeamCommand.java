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
 * Handles all team-related commands including allies
 * Usage: /team <create|invite|accept|leave|kick|disband|color|bold|italic|ally>
 */
public class TeamCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final TeamManager trust;

    public TeamCommand(ElementPlugin plugin, TeamManager trust) {
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
            sendTeamUsage(p);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleTeamCreate(p, args);
            case "invite" -> handleTeamInvite(p, args);
            case "accept", "join" -> handleTeamAccept(p, args);
            case "leave" -> handleTeamLeave(p);
            case "kick" -> handleTeamKick(p, args);
            case "disband" -> handleTeamDisband(p);
            case "color", "colour" -> handleTeamColor(p, args);
            case "bold" -> handleTeamBold(p);
            case "italic" -> handleTeamItalic(p);
            case "ally" -> handleAlly(p, args);
            case "list" -> handleList(p);
            default -> sendTeamUsage(p);
        }

        return true;
    }

    private void handleTeamCreate(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team create <name>", NamedTextColor.RED));
            return;
        }

        String teamName = args[1];

        if (trust.createTeam(p, teamName)) {
            p.sendMessage(Component.text("✓ Created team: ", NamedTextColor.GREEN)
                    .append(Component.text(teamName, NamedTextColor.AQUA, TextDecoration.BOLD)));
            p.sendMessage(Component.text("  Use /team color <color> to customize", NamedTextColor.GRAY));
        }
    }

    private void handleTeamInvite(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team invite <player>", NamedTextColor.RED));
            return;
        }

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        if (trust.inviteToTeam(p, target, teamName)) {
            Component msg = Component.text(p.getName() + " invited you to team ", NamedTextColor.GOLD)
                    .append(Component.text(teamName, NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(". ", NamedTextColor.GOLD))
                    .append(Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                            .clickEvent(ClickEvent.runCommand("/team accept " + teamName))
                            .hoverEvent(HoverEvent.showText(Component.text("Click to join " + teamName))));
            target.sendMessage(msg);

            p.sendMessage(Component.text("✓ Sent team invite to " + target.getName(), NamedTextColor.GREEN));
        }
    }

    private void handleTeamAccept(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team accept <team_name>", NamedTextColor.RED));
            return;
        }

        String teamName = args[1];

        if (trust.acceptTeamInvite(p, teamName)) {
            p.sendMessage(Component.text("✓ Joined team: ", NamedTextColor.GREEN)
                    .append(Component.text(teamName, NamedTextColor.AQUA, TextDecoration.BOLD)));

            for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && !member.equals(p)) {
                    member.sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                            .append(Component.text(p.getName(), NamedTextColor.AQUA))
                            .append(Component.text(" joined the team!", NamedTextColor.GREEN)));
                }
            }
        }
    }

    private void handleTeamLeave(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        boolean isLeader = trust.isTeamLeader(p.getUniqueId(), teamName);

        if (trust.leaveTeam(p)) {
            if (isLeader) {
                p.sendMessage(Component.text("✓ Team disbanded (you were the leader)", NamedTextColor.YELLOW));
            } else {
                p.sendMessage(Component.text("✓ Left team: " + teamName, NamedTextColor.YELLOW));

                for (UUID memberUUID : trust.getTeamMembers(teamName)) {
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null) {
                        member.sendMessage(Component.text(p.getName() + " left the team", NamedTextColor.YELLOW));
                    }
                }
            }
        }
    }

    private void handleTeamKick(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team kick <player>", NamedTextColor.RED));
            return;
        }

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            p.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }

        if (trust.kickFromTeam(p, target, teamName)) {
            p.sendMessage(Component.text("✓ Kicked " + target.getName() + " from the team", NamedTextColor.GREEN));
            target.sendMessage(Component.text("You were kicked from team: " + teamName, NamedTextColor.RED));
        }
    }

    private void handleTeamDisband(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        if (!trust.isTeamLeader(p.getUniqueId(), teamName)) {
            p.sendMessage(Component.text("Only the team leader can disband the team", NamedTextColor.RED));
            return;
        }

        for (UUID memberUUID : trust.getTeamMembers(teamName)) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                member.sendMessage(Component.text("Team " + teamName + " has been disbanded", NamedTextColor.RED));
            }
        }

        trust.disbandTeam(teamName);
        p.sendMessage(Component.text("✓ Team disbanded", NamedTextColor.YELLOW));
    }

    private void handleTeamColor(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team color <color>", NamedTextColor.RED));
            p.sendMessage(Component.text("Examples: red, blue, #FF5733", NamedTextColor.GRAY));
            return;
        }

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        trust.setTeamColor(p, teamName, args[1]);
    }

    private void handleTeamBold(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        trust.toggleTeamBold(p, teamName);
    }

    private void handleTeamItalic(Player p) {
        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName == null) {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.RED));
            return;
        }

        trust.toggleTeamItalic(p, teamName);
    }

    private void handleAlly(Player p, String[] args) {
        if (args.length < 2) {
            p.sendMessage(Component.text("Usage: /team ally <request|accept|remove> <team_name>", NamedTextColor.RED));
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            case "request" -> handleAllyRequest(p, args);
            case "accept" -> handleAllyAccept(p, args);
            case "remove" -> handleAllyRemove(p, args);
            default -> p.sendMessage(Component.text("Usage: /team ally <request|accept|remove> <team_name>", NamedTextColor.RED));
        }
    }

    private void handleAllyRequest(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(Component.text("Usage: /team ally request <team_name>", NamedTextColor.RED));
            return;
        }

        String targetTeamName = args[2];
        trust.requestAlly(p, targetTeamName);
    }

    private void handleAllyAccept(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(Component.text("Usage: /team ally accept <team_name>", NamedTextColor.RED));
            return;
        }

        String requestingTeamName = args[2];
        trust.acceptAlly(p, requestingTeamName);
    }

    private void handleAllyRemove(Player p, String[] args) {
        if (args.length < 3) {
            p.sendMessage(Component.text("Usage: /team ally remove <team_name>", NamedTextColor.RED));
            return;
        }

        String allyTeamName = args[2];
        trust.removeAlly(p, allyTeamName);
    }

    private void handleList(Player p) {
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));

        String teamName = trust.getPlayerTeam(p.getUniqueId());
        if (teamName != null) {
            boolean isLeader = trust.isTeamLeader(p.getUniqueId(), teamName);

            p.sendMessage(Component.text("⚔ Your Team: ", NamedTextColor.GOLD, TextDecoration.BOLD)
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

            // Show allies
            Set<String> allies = trust.getAlliedTeams(p.getUniqueId());
            if (!allies.isEmpty()) {
                p.sendMessage(Component.empty());
                p.sendMessage(Component.text("🤝 Allied Teams:", NamedTextColor.GREEN));
                for (String allyTeam : allies) {
                    p.sendMessage(Component.text("  • " + allyTeam, NamedTextColor.WHITE));
                }
            }

            // Show pending ally requests
            Set<String> pendingAllyRequests = trust.getPendingAllyRequests(p.getUniqueId());
            if (!pendingAllyRequests.isEmpty() && isLeader) {
                p.sendMessage(Component.empty());
                p.sendMessage(Component.text("📨 Pending Ally Requests:", NamedTextColor.YELLOW));
                for (String requestingTeam : pendingAllyRequests) {
                    Component requestMsg = Component.text("  • " + requestingTeam + " ", NamedTextColor.WHITE)
                            .append(Component.text("[ACCEPT]", NamedTextColor.GREEN, TextDecoration.BOLD)
                                    .clickEvent(ClickEvent.runCommand("/team ally accept " + requestingTeam))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to ally with " + requestingTeam))));
                    p.sendMessage(requestMsg);
                }
            }

            p.sendMessage(Component.empty());
        } else {
            p.sendMessage(Component.text("You are not in a team", NamedTextColor.YELLOW));
            p.sendMessage(Component.text("Use /team create <name> to create one", NamedTextColor.GRAY));
        }

        // Show pending team invites
        var pendingInvites = trust.getPendingInvites(p.getUniqueId());
        if (!pendingInvites.isEmpty()) {
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
    }

    private void sendTeamUsage(Player p) {
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        p.sendMessage(Component.text("⚔ Team Commands", NamedTextColor.GOLD, TextDecoration.BOLD));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("  /team list ", NamedTextColor.AQUA)
                .append(Component.text("- View your team info", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team create <name> ", NamedTextColor.AQUA)
                .append(Component.text("- Create a team", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team invite <player> ", NamedTextColor.AQUA)
                .append(Component.text("- Invite to your team", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team accept <name> ", NamedTextColor.AQUA)
                .append(Component.text("- Accept team invite", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team leave ", NamedTextColor.AQUA)
                .append(Component.text("- Leave your team", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team kick <player> ", NamedTextColor.AQUA)
                .append(Component.text("- Kick from team (leader)", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team disband ", NamedTextColor.AQUA)
                .append(Component.text("- Disband team (leader)", NamedTextColor.GRAY)));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("Allies:", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("  /team ally request <team> ", NamedTextColor.AQUA)
                .append(Component.text("- Request ally", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team ally accept <team> ", NamedTextColor.AQUA)
                .append(Component.text("- Accept ally request", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team ally remove <team> ", NamedTextColor.AQUA)
                .append(Component.text("- Remove ally", NamedTextColor.GRAY)));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("Customization (Leader Only):", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("  /team color <color> ", NamedTextColor.AQUA)
                .append(Component.text("- Set team color", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team bold ", NamedTextColor.AQUA)
                .append(Component.text("- Toggle bold formatting", NamedTextColor.GRAY)));
        p.sendMessage(Component.text("  /team italic ", NamedTextColor.AQUA)
                .append(Component.text("- Toggle italic formatting", NamedTextColor.GRAY)));
        p.sendMessage(Component.empty());
        p.sendMessage(Component.text("ℹ Team members and allies cannot hurt each other", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> teamCommands = Arrays.asList("list", "create", "invite", "accept", "join", "leave",
                    "kick", "disband", "color", "bold", "italic", "ally");
            return teamCommands.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if ("invite".equals(args[0].toLowerCase()) || "kick".equals(args[0].toLowerCase())) {
                return getOnlinePlayers(args[1]);
            }
            if ("color".equals(args[0].toLowerCase())) {
                return Arrays.asList("red", "blue", "green", "yellow", "aqua", "gold", "light_purple",
                        "dark_red", "dark_blue", "#FF5733");
            }
            if ("ally".equals(args[0].toLowerCase())) {
                return Arrays.asList("request", "accept", "remove");
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