package hs.elementPlugin.elements;

import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.entity.Player;

/**
 * Context object that encapsulates all managers required for element abilities.
 * This reduces parameter complexity and makes it easier to extend functionality.
 */
public class ElementContext {
    private final Player player;
    private final int upgradeLevel;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final ConfigManager configManager;
    
    public ElementContext(Player player, int upgradeLevel, ManaManager manaManager,
                          TrustManager trustManager,
                         ConfigManager configManager) {
        this.player = player;
        this.upgradeLevel = upgradeLevel;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
        this.configManager = configManager;
    }
    
    public Player getPlayer() { return player; }
    public int getUpgradeLevel() { return upgradeLevel; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public ConfigManager getConfigManager() { return configManager; }
}