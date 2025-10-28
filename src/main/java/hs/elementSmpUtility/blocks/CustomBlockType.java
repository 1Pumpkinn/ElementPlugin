package hs.elementSmpUtility.blocks;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Represents a custom block type with its properties
 */
public class CustomBlockType {

    private final String id;
    private final Material material;
    private final Component displayName;
    private final boolean unbreakable;

    public CustomBlockType(String id, Material material, Component displayName, boolean unbreakable) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.unbreakable = unbreakable;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }
}