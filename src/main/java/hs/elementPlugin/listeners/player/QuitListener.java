package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class QuitListener implements Listener {
    private final ElementPlugin plugin;
    private final ManaManager mana;

    public QuitListener(ElementPlugin plugin, ManaManager mana) {
        this.plugin = plugin;
        this.mana = mana;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        // CRITICAL: Cancel any ongoing rolling animation
        plugin.getElementManager().cancelRolling(player);

        // Save mana data
        mana.save(uuid);

        // CRITICAL: Clear ALL element effects before player logs out
        // This prevents effects from persisting after reroll
        clearAllElementEffects(player);

        // Note: Player's actual health is automatically saved by Minecraft
        // No need to manually save it - it will restore on rejoin

        // Save player data
        plugin.getDataStore().save(plugin.getDataStore().getPlayerData(uuid));
    }

    /**
     * Clear ALL possible element effects from a player on logout
     * This prevents old element effects from persisting after reroll
     */
    private void clearAllElementEffects(Player player) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        // Clear effects from ALL elements (not just current)
        for (ElementType type : ElementType.values()) {
            Element element = plugin.getElementManager().get(type);
            if (element != null) {
                element.clearEffects(player);
            }
        }

        // Reset Life max health if player is NOT Life element
        // Their actual health value is preserved by Minecraft automatically
        if (currentElement != ElementType.LIFE) {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() > 20.0) {
                attr.setBaseValue(20.0);
                // Don't touch their health - Minecraft will cap it automatically on rejoin
            }
        }
    }
}