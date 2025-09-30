package hs.elementPlugin.data;

import hs.elementPlugin.elements.ElementType;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private ElementType currentElement;
    private final Map<ElementType, Integer> upgrades = new EnumMap<>(ElementType.class); // 0..2
    private final java.util.EnumSet<ElementType> ownedItems = java.util.EnumSet.noneOf(ElementType.class);
    private int mana = 100;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }

    public ElementType getCurrentElement() { return currentElement; }

    public void setCurrentElement(ElementType currentElement) { this.currentElement = currentElement; }

    public int getUpgradeLevel(ElementType type) {
        return upgrades.getOrDefault(type, 0);
    }

    public void setUpgradeLevel(ElementType type, int level) {
        upgrades.put(type, Math.max(0, Math.min(2, level)));
    }

    public Map<ElementType, Integer> getUpgradesView() { return upgrades; }

    public java.util.Set<ElementType> getOwnedItems() { return ownedItems; }

    public boolean hasElementItem(ElementType type) { return ownedItems.contains(type); }

    public void addElementItem(ElementType type) { ownedItems.add(type); }

    public int getMana() { return mana; }

    public void setMana(int mana) { this.mana = Math.max(0, mana); }

    public void addMana(int delta) { setMana(this.mana + delta); }
}