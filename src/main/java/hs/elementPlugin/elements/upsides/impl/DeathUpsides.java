package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public class DeathUpsides extends BaseUpsides {

    public DeathUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.DEATH;
    }

    /**
     * Apply all Death element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Death element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Raw/undead foods act as golden apples (handled in listener)
        // Upside 2: Nearby enemies get hunger 1 in 5x5 radius (upgrade level 2+)
        if (upgradeLevel >= 2) {
            applyHungerToNearbyEnemies(player);
        }
    }

    /**
     * Check if a food item should act as a golden apple for this player
     * @param player The player consuming the food
     * @param foodType The type of food being consumed
     * @return true if the food should act as a golden apple
     */
    public boolean shouldActAsGoldenApple(Player player, Material foodType) {
        if (!hasElement(player)) {
            return false;
        }
        return isRawOrUndeadFood(foodType);
    }

    /**
     * Apply golden apple effects to a player
     * @param player The player to apply effects to
     */
    public void applyGoldenAppleEffects(Player player) {
        // Apply regular golden apple effects (not enchanted)
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1)); // 5s regen II
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0)); // 2 min absorption I
    }

    /**
     * Check if a food item is raw or undead food
     * @param food The food material to check
     * @return true if it's raw or undead food
     */
    private boolean isRawOrUndeadFood(Material food) {
        return food == Material.ROTTEN_FLESH || 
               food == Material.CHICKEN || 
               food == Material.BEEF || 
               food == Material.PORKCHOP || 
               food == Material.MUTTON || 
               food == Material.RABBIT || 
               food == Material.COD || 
               food == Material.SALMON;
    }

    /**
     * Apply hunger 1 effect to nearby enemies in 5x5 radius
     * @param player The Death element player
     */
    private void applyHungerToNearbyEnemies(Player player) {
        Collection<LivingEntity> nearby = player.getLocation().getNearbyLivingEntities(5.0);
        for (LivingEntity entity : nearby) {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, true, true)); // 3 seconds
            }
        }
    }
}
