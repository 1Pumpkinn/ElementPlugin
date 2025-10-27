package hs.event.LifeDeathEvent;

import hs.elementPlugin.ElementPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Main manager for the Life/Death event system
 * Handles initialization, cleanup, and player join/quit
 *
 * NOTE: This is a temporary event system. When the event is over,
 * the entire hs.event.LifeDeathEvent package can be safely deleted.
 */
public class EventManager implements Listener {
    private final ElementPlugin plugin;
    private final PointSystem pointSystem;
    private final MessageSystem messageSystem;
    private final PointScoreboard scoreboard;
    private final PassiveMobListener passiveListener;
    private final HostileMobListener hostileListener;
    private final StartEventCommand command;

    public EventManager(ElementPlugin plugin) {
        this.plugin = plugin;

        // Initialize systems
        this.pointSystem = new PointSystem(plugin);
        this.messageSystem = new MessageSystem();
        this.scoreboard = new PointScoreboard(plugin, pointSystem);

        // Initialize listeners
        this.passiveListener = new PassiveMobListener(pointSystem, messageSystem);
        this.hostileListener = new HostileMobListener(pointSystem, messageSystem);

        // Initialize command
        this.command = new StartEventCommand(plugin, pointSystem, messageSystem, scoreboard);

        // Register everything
        register();
    }

    /**
     * Register all event components
     */
    private void register() {
        // Register listeners
        Bukkit.getPluginManager().registerEvents(passiveListener, plugin);
        Bukkit.getPluginManager().registerEvents(hostileListener, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);

        // Register command
        plugin.getCommand("lifedeath").setExecutor(command);
        plugin.getCommand("lifedeath").setTabCompleter(command);

        plugin.getLogger().info("Life vs Death event system initialized");

        // If event was active when server stopped, restore scoreboards
        if (pointSystem.isEventActive()) {
            plugin.getLogger().info("Restoring event from previous session");
            for (Player player : Bukkit.getOnlinePlayers()) {
                scoreboard.showScoreboard(player);
            }
        }
    }

    /**
     * Handle player join - show scoreboard if event is active
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (pointSystem.isEventActive()) {
            // Delay slightly to ensure player is fully loaded
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    scoreboard.showScoreboard(event.getPlayer());
                }
            }, 20L);
        }
    }

    /**
     * Handle player quit - save their scores
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Scores are saved automatically by PointSystem
        // Just hide the scoreboard
        scoreboard.hideScoreboard(event.getPlayer());
    }

    /**
     * Cleanup when plugin disables
     */
    public void cleanup() {
        // Save all data
        pointSystem.saveData();

        // Clean up scoreboards
        scoreboard.cleanup();

        plugin.getLogger().info("Life vs Death event system cleaned up");
    }

    /**
     * Get the point system
     */
    public PointSystem getPointSystem() {
        return pointSystem;
    }

    /**
     * Get the message system
     */
    public MessageSystem getMessageSystem() {
        return messageSystem;
    }

    /**
     * Get the scoreboard
     */
    public PointScoreboard getScoreboard() {
        return scoreboard;
    }
}