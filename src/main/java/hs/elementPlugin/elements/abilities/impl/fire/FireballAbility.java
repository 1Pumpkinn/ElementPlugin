package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Fire element's fireball ability - launches a fireball that damages entities
 * Can also be used for rocket-jumping when looking down
 */
public class FireballAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public FireballAbility(ElementPlugin plugin) {
        super("fire_fireball", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        // Check if player is looking down (pitch > 45 degrees down)
        // Pitch: -90 = straight up, 0 = forward, 90 = straight down
        float pitch = player.getLocation().getPitch();
        boolean isLookingDown = pitch > 45;

        if (isLookingDown) {
            // ROCKET JUMP MODE: Boost player upward and forward
            executeRocketJump(player, direction);
        } else {
            // NORMAL MODE: Launch fireball
            executeFireball(player, direction);
        }

        return true;
    }

    /**
     * Execute normal fireball launch
     */
    private void executeFireball(Player player, Vector direction) {
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(2.0));

        // Set fireball properties
        fireball.setShooter(player);
        fireball.setYield(0.0f); // No explosion power - won't destroy terrain
        fireball.setIsIncendiary(false); // Don't set blocks on fire

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
    }

    /**
     * Execute rocket jump boost
     */
    private void executeRocketJump(Player player, Vector direction) {
        // Calculate boost vector: opposite of look direction + strong upward component
        Vector boost = direction.clone().multiply(-1.5); // Reverse direction
        boost.setY(1.2); // Strong upward boost

        // Apply velocity to player
        player.setVelocity(boost);

        // Spawn explosion-like effects at player's feet
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.5, 0.2, 0.5, 0.15, null, true);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation(), 10, 0.3, 0.1, 0.3, 0.0, null, true);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.4, 0.2, 0.4, 0.05, null, true);

        // Play explosion sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);

        // Send feedback to player
        player.sendActionBar(
                net.kyori.adventure.text.Component.text("ROCKET JUMP!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
        );
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Fireball";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Launch a fireball that damages enemies. Look down to rocket jump! (50 mana)";
    }
}