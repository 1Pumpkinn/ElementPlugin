package saturn.elementPlugin.elements.abilities.impl.air;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import saturn.elementPlugin.managers.ManaManager;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirBlastAbility extends BaseAbility {

    private final ElementPlugin plugin;

    public AirBlastAbility(ElementPlugin plugin) {
        super("air_blast", 50, 8, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        ManaManager mana = context.getManaManager();

        int cost = 20;
        if (!mana.hasMana(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }

        mana.takeMana(player, cost);

        double radius = 6.0;
        World world = player.getWorld();
        Location center = player.getLocation();

        // Initial particle ring
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = Math.cos(rad) * 1.5;
            double z = Math.sin(rad) * 1.5;
            world.spawnParticle(
                    Particle.CLOUD,
                    center.clone().add(x, 0.2, z),
                    2, 0, 0, 0, 0,
                    null,
                    true
            );
        }

        // Expanding particle wave
        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                double currentRadius = 1.5 + (tick * 0.8);
                if (currentRadius > 8.0) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    int count = Math.max(1, 3 - tick / 2);
                    world.spawnParticle(
                            Particle.CLOUD,
                            center.clone().add(x, 0.2, z),
                            count, 0, 0, 0, 0,
                            null,
                            true
                    );
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Launch nearby entities
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;
            if (!isValidTarget(context, entity)) continue;

            Vector push = entity.getLocation().toVector()
                    .subtract(center.toVector())
                    .normalize()
                    .multiply(2.25)
                    .setY(1.5);

            entity.setVelocity(push);
        }

        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.WHITE + "Air Blast";
    }

    @Override
    public String getDescription() {
        return "Create a powerful blast of air that pushes enemies away from you.";
    }
}
