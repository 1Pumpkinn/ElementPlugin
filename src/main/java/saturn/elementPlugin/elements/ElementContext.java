package saturn.elementPlugin.elements;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.ManaManager;
import saturn.elementPlugin.managers.TrustManager;
import org.bukkit.entity.Player;

/**
 * Context object that encapsulates all managers required for element abilities.
 * Uses builder pattern for flexible construction.
 */
public class ElementContext {
    private final Player player;
    private final int upgradeLevel;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final ElementType elementType;
    private final ElementPlugin plugin;

    private ElementContext(Builder builder) {
        this.player = builder.player;
        this.upgradeLevel = builder.upgradeLevel;
        this.elementType = builder.elementType;
        this.manaManager = builder.manaManager;
        this.trustManager = builder.trustManager;
        this.plugin = builder.plugin;
    }

    // Getters
    public Player getPlayer() { return player; }
    public int getUpgradeLevel() { return upgradeLevel; }
    public ElementType getElementType() { return elementType; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public ElementPlugin getPlugin() { return plugin; }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Player player;
        private int upgradeLevel;
        private ElementType elementType;
        private ManaManager manaManager;
        private TrustManager trustManager;
        private ElementPlugin plugin;

        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Builder upgradeLevel(int level) {
            this.upgradeLevel = level;
            return this;
        }

        public Builder elementType(ElementType type) {
            this.elementType = type;
            return this;
        }

        public Builder manaManager(ManaManager manager) {
            this.manaManager = manager;
            return this;
        }

        public Builder trustManager(TrustManager manager) {
            this.trustManager = manager;
            return this;
        }

        public Builder plugin(ElementPlugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public ElementContext build() {
            return new ElementContext(this);
        }
    }
}