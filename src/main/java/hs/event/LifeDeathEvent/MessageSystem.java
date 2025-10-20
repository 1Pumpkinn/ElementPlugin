package hs.event.LifeDeathEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.UUID;

/**
 * Handles all messaging for the Life/Death event
 */
public class MessageSystem {

    /**
     * Send passive kill message to player
     */
    public void sendPassiveKillMessage(Player player, int totalKills) {
        player.sendActionBar(
                Component.text("🌿 Passive Kills: ")
                        .color(NamedTextColor.GREEN)
                        .append(Component.text(totalKills, NamedTextColor.WHITE))
        );
    }

    /**
     * Send hostile kill message to player
     */
    public void sendHostileKillMessage(Player player, int totalKills) {
        player.sendActionBar(
                Component.text("💀 Hostile Kills: ")
                        .color(NamedTextColor.DARK_PURPLE)
                        .append(Component.text(totalKills, NamedTextColor.WHITE))
        );
    }

    /**
     * Broadcast event start
     */
    public void broadcastEventStart() {
        Component message = Component.text()
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("⚔ LIFE vs DEATH EVENT STARTED ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("🌿 ", NamedTextColor.GREEN))
                .append(Component.text("Kill ", NamedTextColor.WHITE))
                .append(Component.text("PASSIVE MOBS", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" to earn the ", NamedTextColor.WHITE))
                .append(Component.text("LIFE", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" element!", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.text("💀 ", NamedTextColor.DARK_PURPLE))
                .append(Component.text("Kill ", NamedTextColor.WHITE))
                .append(Component.text("HOSTILE MOBS", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text(" to earn the ", NamedTextColor.WHITE))
                .append(Component.text("DEATH", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" element!", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("Top killer in each category wins!", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build();

        Bukkit.broadcast(message);

        // Play sound to all players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    /**
     * Broadcast event end and winners
     */
    public void broadcastEventEnd(UUID lifeWinner, int lifeKills, UUID deathWinner, int deathKills) {
        Player lifePlayer = Bukkit.getPlayer(lifeWinner);
        Player deathPlayer = Bukkit.getPlayer(deathWinner);

        String lifeName = lifePlayer != null ? lifePlayer.getName() : "Unknown";
        String deathName = deathPlayer != null ? deathPlayer.getName() : "Unknown";

        Component message = Component.text()
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("⚔ LIFE vs DEATH EVENT ENDED ⚔", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("🌿 LIFE WINNER: ", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(lifeName, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" with ", NamedTextColor.WHITE))
                .append(Component.text(lifeKills + " kills", NamedTextColor.GREEN))
                .append(Component.newline())
                .append(Component.text("💀 DEATH WINNER: ", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(deathName, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text(" with ", NamedTextColor.WHITE))
                .append(Component.text(deathKills + " kills", NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text("═══════════════════════════════", NamedTextColor.GOLD, TextDecoration.BOLD))
                .build();

        Bukkit.broadcast(message);

        // Show title to winners
        if (lifePlayer != null) {
            Title lifeTitle = Title.title(
                    Component.text("🌿 LIFE ELEMENT 🌿", NamedTextColor.GREEN, TextDecoration.BOLD),
                    Component.text("You are the champion!", NamedTextColor.LIGHT_PURPLE),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
            );
            lifePlayer.showTitle(lifeTitle);
            lifePlayer.playSound(lifePlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        if (deathPlayer != null) {
            Title deathTitle = Title.title(
                    Component.text("💀 DEATH ELEMENT 💀", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD),
                    Component.text("You are the champion!", NamedTextColor.RED),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500))
            );
            deathPlayer.showTitle(deathTitle);
            deathPlayer.playSound(deathPlayer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    /**
     * Send error message when trying to start event while already active
     */
    public void sendEventAlreadyActive(Player player) {
        player.sendMessage(
                Component.text("Event is already active!", NamedTextColor.RED)
        );
    }

    /**
     * Send error message when trying to end event that isn't active
     */
    public void sendEventNotActive(Player player) {
        player.sendMessage(
                Component.text("No event is currently active!", NamedTextColor.RED)
        );
    }

    /**
     * Send error when no kills were recorded
     */
    public void sendNoKillsRecorded(Player player, String category) {
        player.sendMessage(
                Component.text("No " + category + " kills recorded!", NamedTextColor.YELLOW)
        );
    }
}