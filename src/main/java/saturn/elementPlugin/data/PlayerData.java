package saturn.elementPlugin.data;

import saturn.elementPlugin.elements.ElementType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Represents all data for a single player.
 * This class is immutable-friendly with clear getters/setters.
 * UPDATED: Removed trust system data (moved to TeamData)
 */
public class PlayerData {
    // Core identity
    private final UUID uuid;

    // Element data
    private ElementType currentElement;
    private int currentElementUpgradeLevel = 0;
    private final EnumSet<ElementType> ownedItems = EnumSet.noneOf(ElementType.class);

    // Mana system
    private int mana = 100;

    // Dirty flag for efficient saving
    private transient boolean dirty = false;

    /**
     * Create a new PlayerData with default values
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Load PlayerData from configuration section
     */
    public PlayerData(UUID uuid, ConfigurationSection section) {
        this.uuid = uuid;

        if (section == null) {
            return; // Use defaults
        }

        // Load element
        String elementString = section.getString("element");
        if (elementString != null) {
            try {
                this.currentElement = ElementType.valueOf(elementString);
            } catch (IllegalArgumentException e) {
                // Invalid element in config - use null (will get assigned on join)
            }
        }

        // Load mana
        this.mana = Math.max(0, section.getInt("mana", 100));

        // Load upgrade level
        this.currentElementUpgradeLevel = Math.max(0, Math.min(2, section.getInt("currentUpgradeLevel", 0)));

        // Load owned items
        for (String itemName : section.getStringList("items")) {
            try {
                ElementType type = ElementType.valueOf(itemName);
                this.ownedItems.add(type);
            } catch (IllegalArgumentException e) {
                // Skip invalid items
            }
        }

        // Data loaded from disk is clean
        this.dirty = false;
    }

    // ========================================
    // CORE IDENTITY
    // ========================================

    public UUID getUuid() {
        return uuid;
    }

    // ========================================
    // ELEMENT MANAGEMENT
    // ========================================

    public ElementType getCurrentElement() {
        return currentElement;
    }

    /**
     * Alias for getCurrentElement() for backwards compatibility
     */
    public ElementType getElementType() {
        return currentElement;
    }

    /**
     * Set current element and reset upgrade level to 0
     */
    public void setCurrentElement(ElementType element) {
        if (this.currentElement != element) {
            this.currentElement = element;
            this.currentElementUpgradeLevel = 0; // Reset level when switching
            markDirty();
        }
    }

    /**
     * Set current element WITHOUT resetting upgrade level
     * Used when loading from disk or rerolling with preserved upgrades
     */
    public void setCurrentElementWithoutReset(ElementType element) {
        if (this.currentElement != element) {
            this.currentElement = element;
            markDirty();
        }
    }

    // ========================================
    // UPGRADE SYSTEM
    // ========================================

    public int getCurrentElementUpgradeLevel() {
        return currentElementUpgradeLevel;
    }

    public void setCurrentElementUpgradeLevel(int level) {
        int newLevel = Math.max(0, Math.min(2, level)); // Clamp 0-2
        if (this.currentElementUpgradeLevel != newLevel) {
            this.currentElementUpgradeLevel = newLevel;
            markDirty();
        }
    }

    /**
     * Get upgrade level for a specific element
     * Only returns non-zero for the current element
     */
    public int getUpgradeLevel(ElementType type) {
        if (type != null && type.equals(currentElement)) {
            return currentElementUpgradeLevel;
        }
        return 0;
    }

    /**
     * Set upgrade level for a specific element
     * Only sets for the current element
     */
    public void setUpgradeLevel(ElementType type, int level) {
        if (type != null && type.equals(currentElement)) {
            setCurrentElementUpgradeLevel(level);
        }
    }

    /**
     * Get a view of all upgrades (for display purposes)
     */
    public java.util.Map<ElementType, Integer> getUpgradesView() {
        java.util.Map<ElementType, Integer> map = new java.util.EnumMap<>(ElementType.class);
        if (currentElement != null) {
            map.put(currentElement, currentElementUpgradeLevel);
        }
        return map;
    }

    // ========================================
    // OWNED ITEMS
    // ========================================

    public java.util.Set<ElementType> getOwnedItems() {
        return ownedItems;
    }

    public boolean hasElementItem(ElementType type) {
        return ownedItems.contains(type);
    }

    public void addElementItem(ElementType type) {
        if (ownedItems.add(type)) {
            markDirty();
        }
    }

    public void removeElementItem(ElementType type) {
        if (ownedItems.remove(type)) {
            markDirty();
        }
    }

    // ========================================
    // MANA SYSTEM
    // ========================================

    public int getMana() {
        return mana;
    }

    public void setMana(int mana) {
        int newMana = Math.max(0, mana);
        if (this.mana != newMana) {
            this.mana = newMana;
            markDirty();
        }
    }

    public void addMana(int delta) {
        setMana(this.mana + delta);
    }

    // ========================================
    // DIRTY FLAG SYSTEM
    // ========================================

    /**
     * Check if this data has unsaved changes
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Mark this data as having unsaved changes
     */
    private void markDirty() {
        this.dirty = true;
    }

    /**
     * Mark this data as saved (clean)
     */
    public void markClean() {
        this.dirty = false;
    }

    // ========================================
    // SERIALIZATION
    // ========================================

    /**
     * Save this PlayerData to a ConfigurationSection
     */
    public void saveTo(ConfigurationSection section) {
        section.set("element", currentElement == null ? null : currentElement.name());
        section.set("mana", mana);
        section.set("currentUpgradeLevel", currentElementUpgradeLevel);

        // Save owned items
        java.util.List<String> itemNames = new java.util.ArrayList<>();
        for (ElementType type : ownedItems) {
            itemNames.add(type.name());
        }
        section.set("items", itemNames);

        markClean();
    }

    // ========================================
    // DEBUGGING
    // ========================================

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", element=" + currentElement +
                ", upgradeLevel=" + currentElementUpgradeLevel +
                ", mana=" + mana +
                ", ownedItems=" + ownedItems.size() +
                ", dirty=" + dirty +
                '}';
    }
}