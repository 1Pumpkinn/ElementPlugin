package hs.elementSmpUtility.blocks;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;

/**
 * Extension of CustomBlockType that includes custom model data
 */
public class CustomModelBlock extends CustomBlockType {

    private final int customModelData;

    public CustomModelBlock(String id, Material material, Component displayName,
                            boolean unbreakable, int customModelData) {
        super(id, material, displayName, unbreakable);
        this.customModelData = customModelData;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean hasCustomModel() {
        return customModelData > 0;
    }
}