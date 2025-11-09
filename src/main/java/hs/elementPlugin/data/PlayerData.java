package hs.elementPlugin.data;

import hs.elementPlugin.elements.ElementType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private ElementType currentElement;
    private final java.util.EnumSet<ElementType> ownedItems = java.util.EnumSet.noneOf(ElementType.class);
    private int mana = 100;
    private int currentElementUpgradeLevel = 0; // Only save current element's upgrade level
    private final Set<UUID> trustedPlayers = new HashSet<>();

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public PlayerData(UUID uuid, org.bukkit.configuration.ConfigurationSection section) {
        this.uuid = uuid;
        if (section != null) {
            String elem = section.getString("element");
            if (elem != null) {
                try {
                    this.currentElement = ElementType.valueOf(elem);
                } catch (IllegalArgumentException e) {
                    // Invalid element, leave as null
                }
            }
            this.mana = section.getInt("mana", 100);
            this.currentElementUpgradeLevel = section.getInt("currentUpgradeLevel", 0);
            java.util.List<String> items = section.getStringList("items");
            if (items != null) {
                for (String name : items) {
                    try {
                        ElementType t = ElementType.valueOf(name);
                        this.ownedItems.add(t);
                    } catch (IllegalArgumentException e) {
                        // skip invalid
                    }
                }
            }

            // Load trust list
            org.bukkit.configuration.ConfigurationSection trustSection = section.getConfigurationSection("trust");
            if (trustSection != null) {
                for (String key : trustSection.getKeys(false)) {
                    try {
                        this.trustedPlayers.add(UUID.fromString(key));
                    } catch (IllegalArgumentException e) {
                        // Invalid UUID, skip
                    }
                }
            }
        }
    }

    public UUID getUuid() { return uuid; }

    public int getCurrentElementUpgradeLevel() { return currentElementUpgradeLevel; }

    public void setCurrentElementUpgradeLevel(int level) {
        this.currentElementUpgradeLevel = Math.max(0, Math.min(2, level));
    }

    public ElementType getCurrentElement() { return currentElement; }

    public ElementType getElementType() { return currentElement; }

    public void setCurrentElement(ElementType currentElement) {
        this.currentElement = currentElement;
        // Reset upgrade level when switching elements (except when loading from save)
        if (currentElement != null) {
            this.currentElementUpgradeLevel = 0;
        }
    }

    public void setCurrentElementWithoutReset(ElementType currentElement) {
        // Used when loading from save - doesn't reset upgrade level
        this.currentElement = currentElement;
    }

    public int getUpgradeLevel(ElementType type) {
        // Only return upgrade level for current element
        if (type != null && type.equals(currentElement)) {
            return currentElementUpgradeLevel;
        }
        return 0;
    }

    public void setUpgradeLevel(ElementType type, int level) {
        // Only set upgrade level for current element
        if (type != null && type.equals(currentElement)) {
            setCurrentElementUpgradeLevel(level);
        }
    }

    public java.util.Map<ElementType, Integer> getUpgradesView() {
        java.util.Map<ElementType, Integer> map = new java.util.EnumMap<>(ElementType.class);
        if (currentElement != null) {
            map.put(currentElement, currentElementUpgradeLevel);
        }
        return map;
    }

    public java.util.Set<ElementType> getOwnedItems() { return ownedItems; }

    public boolean hasElementItem(ElementType type) { return ownedItems.contains(type); }

    public void addElementItem(ElementType type) { ownedItems.add(type); }

    public void removeElementItem(ElementType type) { ownedItems.remove(type); }

    public int getMana() { return mana; }

    public void setMana(int mana) { this.mana = Math.max(0, mana); }

    public void addMana(int delta) { setMana(this.mana + delta); }

    // Trust list methods
    public Set<UUID> getTrustedPlayers() {
        return new HashSet<>(trustedPlayers);
    }

    public void addTrustedPlayer(UUID uuid) {
        trustedPlayers.add(uuid);
    }

    public void removeTrustedPlayer(UUID uuid) {
        trustedPlayers.remove(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        return trustedPlayers.contains(uuid);
    }

    public void setTrustedPlayers(Set<UUID> trusted) {
        trustedPlayers.clear();
        if (trusted != null) {
            trustedPlayers.addAll(trusted);
        }
    }
}