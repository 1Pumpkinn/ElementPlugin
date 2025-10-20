package hs.event.LifeDeathEvent;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ElementCoreItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command to manage the Life/Death event
 */
public class StartEventCommand implements CommandExecutor, TabCompleter {
    private final ElementPlugin plugin;
    private final PointSystem pointSystem;
    private final MessageSystem messageSystem;
    private final PointScoreboard scoreboard;

    public StartEventCommand(ElementPlugin plugin, PointSystem pointSystem, MessageSystem messageSystem, PointScoreboard scoreboard) {
        this.plugin = plugin;
        this.pointSystem = pointSystem;
        this.messageSystem = messageSystem;
        this.scoreboard = scoreboard;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                return handleStart(sender);
            case "end":
                return handleEnd(sender);
            case "status":
                return handleStatus(sender);
            case "leaderboard":
            case "lb":
                return handleLeaderboard(sender);
            case "reset":
                return handleReset(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleStart(CommandSender sender) {
        if (pointSystem.isEventActive()) {
            messageSystem.sendEventAlreadyActive((Player) sender);
            return true;
        }

        pointSystem.startEvent();
        messageSystem.broadcastEventStart();

        // Enable scoreboard for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboard.showScoreboard(player);
        }

        sender.sendMessage(Component.text("Life vs Death event started!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleEnd(CommandSender sender) {
        if (!pointSystem.isEventActive()) {
            messageSystem.sendEventNotActive((Player) sender);
            return true;
        }

        // Get winners
        UUID lifeWinner = pointSystem.getTopPassivePlayer();
        UUID deathWinner = pointSystem.getTopHostilePlayer();

        if (lifeWinner == null && deathWinner == null) {
            sender.sendMessage(Component.text("No kills were recorded during this event!", NamedTextColor.RED));
            pointSystem.endEvent();
            for (Player player : Bukkit.getOnlinePlayers()) {
                scoreboard.hideScoreboard(player);
            }
            return true;
        }

        int lifeKills = lifeWinner != null ? pointSystem.getPassiveKills(lifeWinner) : 0;
        int deathKills = deathWinner != null ? pointSystem.getHostileKills(deathWinner) : 0;

        // Award Life element if there's a winner
        if (lifeWinner != null) {
            Player lifePlayer = Bukkit.getPlayer(lifeWinner);

            if (lifePlayer != null && lifePlayer.isOnline()) {
                // Give Life element core
                ItemStack lifeCoreItem = ElementCoreItem.createCore(plugin, ElementType.LIFE);
                if (lifeCoreItem != null) {
                    lifePlayer.getInventory().addItem(lifeCoreItem);
                }

                // Mark that they've crafted Life element
                plugin.getDataStore().setLifeElementCrafted(true);
            }
        }

        // Award Death element if there's a winner
        if (deathWinner != null) {
            Player deathPlayer = Bukkit.getPlayer(deathWinner);

            if (deathPlayer != null && deathPlayer.isOnline()) {
                // Give Death element core
                ItemStack deathCoreItem = ElementCoreItem.createCore(plugin, ElementType.DEATH);
                if (deathCoreItem != null) {
                    deathPlayer.getInventory().addItem(deathCoreItem);
                }

                // Mark that they've crafted Death element
                plugin.getDataStore().setDeathElementCrafted(true);
            }
        }

        // Broadcast winners (now happens AFTER awarding elements)
        messageSystem.broadcastEventEnd(lifeWinner, lifeKills, deathWinner, deathKills);

        // End event
        pointSystem.endEvent();

        // Hide scoreboard for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            scoreboard.hideScoreboard(player);
        }

        sender.sendMessage(Component.text("Life vs Death event ended!", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        boolean active = pointSystem.isEventActive();

        sender.sendMessage(Component.text("═══ Event Status ═══", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Active: ", NamedTextColor.YELLOW)
                .append(Component.text(active ? "YES" : "NO", active ? NamedTextColor.GREEN : NamedTextColor.RED)));

        if (active) {
            UUID topPassive = pointSystem.getTopPassivePlayer();
            UUID topHostile = pointSystem.getTopHostilePlayer();

            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("🌿 Top Passive Killer:", NamedTextColor.GREEN));
            if (topPassive != null) {
                Player player = Bukkit.getPlayer(topPassive);
                String name = player != null ? player.getName() : "Offline Player";
                int kills = pointSystem.getPassiveKills(topPassive);
                sender.sendMessage(Component.text("  " + name + " - " + kills + " kills", NamedTextColor.WHITE));
            } else {
                sender.sendMessage(Component.text("  None", NamedTextColor.GRAY));
            }

            sender.sendMessage(Component.text(""));
            sender.sendMessage(Component.text("💀 Top Hostile Killer:", NamedTextColor.DARK_PURPLE));
            if (topHostile != null) {
                Player player = Bukkit.getPlayer(topHostile);
                String name = player != null ? player.getName() : "Offline Player";
                int kills = pointSystem.getHostileKills(topHostile);
                sender.sendMessage(Component.text("  " + name + " - " + kills + " kills", NamedTextColor.WHITE));
            } else {
                sender.sendMessage(Component.text("  None", NamedTextColor.GRAY));
            }
        }

        return true;
    }

    private boolean handleLeaderboard(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Leaderboards ═══", NamedTextColor.GOLD));

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("🌿 Passive Kills:", NamedTextColor.GREEN));

        var passiveLeaders = pointSystem.getPassiveLeaderboard();
        if (passiveLeaders.isEmpty()) {
            sender.sendMessage(Component.text("  No kills yet", NamedTextColor.GRAY));
        } else {
            int rank = 1;
            for (var entry : passiveLeaders) {
                if (rank > 10) break; // Show top 10
                Player player = Bukkit.getPlayer(entry.getKey());
                String name = player != null ? player.getName() : "Offline Player";
                sender.sendMessage(Component.text("  " + rank + ". " + name + " - " + entry.getValue() + " kills", NamedTextColor.WHITE));
                rank++;
            }
        }

        sender.sendMessage(Component.text(""));
        sender.sendMessage(Component.text("💀 Hostile Kills:", NamedTextColor.DARK_PURPLE));

        var hostileLeaders = pointSystem.getHostileLeaderboard();
        if (hostileLeaders.isEmpty()) {
            sender.sendMessage(Component.text("  No kills yet", NamedTextColor.GRAY));
        } else {
            int rank = 1;
            for (var entry : hostileLeaders) {
                if (rank > 10) break; // Show top 10
                Player player = Bukkit.getPlayer(entry.getKey());
                String name = player != null ? player.getName() : "Offline Player";
                sender.sendMessage(Component.text("  " + rank + ". " + name + " - " + entry.getValue() + " kills", NamedTextColor.WHITE));
                rank++;
            }
        }

        return true;
    }

    private boolean handleReset(CommandSender sender) {
        if (pointSystem.isEventActive()) {
            sender.sendMessage(Component.text("Cannot reset scores while event is active!", NamedTextColor.RED));
            return true;
        }

        pointSystem.resetScores();
        sender.sendMessage(Component.text("All scores have been reset!", NamedTextColor.GREEN));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("═══ Life vs Death Event Commands ═══", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/lifedeath start", NamedTextColor.YELLOW)
                .append(Component.text(" - Start the event", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/lifedeath end", NamedTextColor.YELLOW)
                .append(Component.text(" - End the event and award winners", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/lifedeath status", NamedTextColor.YELLOW)
                .append(Component.text(" - Check event status", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/lifedeath leaderboard", NamedTextColor.YELLOW)
                .append(Component.text(" - View top 10 players", NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/lifedeath reset", NamedTextColor.YELLOW)
                .append(Component.text(" - Reset all scores (when inactive)", NamedTextColor.WHITE)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions.add("start");
            completions.add("end");
            completions.add("status");
            completions.add("leaderboard");
            completions.add("reset");

            // Filter based on input
            return completions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}