package saturn.elementPlugin.elements.abilities.impl.metal;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MetalDashAbility extends BaseAbility implements Listener {

    private final ElementPlugin plugin;

    private final Set<UUID> stunnedPlayers = new HashSet<>();
    private final Set<UUID> dashingPlayers = new HashSet<>();
    private final Map<UUID, Boolean> pendingStuns = new HashMap<>();

    public MetalDashAbility(ElementPlugin plugin) {
        super("metal_dash", 50, 15, 1);
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.multiply(2.5).setY(0.4));

        dashingPlayers.add(player.getUniqueId());
        setActive(player, true);

        Set<UUID> hitEntities = new HashSet<>();
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                // HARD CANCEL (includes totem pop)
                if (!player.isOnline() || !dashingPlayers.contains(player.getUniqueId())) {
                    cleanup(player);
                    cancel();
                    return;
                }

                if (ticks >= 40) {
                    cleanup(player);
                    if (!player.isOnGround()) {
                        pendingStuns.put(player.getUniqueId(), true);
                    } else {
                        applyStun(player);
                    }
                    cancel();
                    return;
                }

                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1);

                if (ticks % 2 == 0) {
                    for (LivingEntity entity : loc.getNearbyLivingEntities(2.5)) {
                        if (entity == player) continue;
                        if (hitEntities.contains(entity.getUniqueId())) continue;

                        // STOP dash if entity is invulnerable (totem pop)
                        if (entity.getNoDamageTicks() > 0) {
                            cleanup(player);
                            cancel();
                            return;
                        }

                        double damage = 4.0;
                        double health = entity.getHealth();

                        if (health - damage <= 0) {
                            entity.damage(damage, player);
                        } else {
                            entity.setHealth(health - damage);
                            entity.damage(0.0);
                        }

                        hitEntities.add(entity.getUniqueId());
                        entity.setVelocity(
                                entity.getLocation().toVector()
                                        .subtract(loc.toVector())
                                        .normalize()
                                        .setY(0.3)
                                        .multiply(0.5)
                        );

                        entity.getWorld().playSound(
                                entity.getLocation(),
                                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                                1f,
                                1.2f
                        );
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    public void cancelDash(Player player) {
        cleanup(player);
    }

    private void cleanup(Player player) {
        dashingPlayers.remove(player.getUniqueId());
        pendingStuns.remove(player.getUniqueId());
        stunnedPlayers.remove(player.getUniqueId());
        setActive(player, false);
    }

    private void applyStun(Player player) {
        stunnedPlayers.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1f, 0.8f);

        new BukkitRunnable() {
            @Override
            public void run() {
                stunnedPlayers.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (pendingStuns.containsKey(id) && player.isOnGround()) {
            pendingStuns.remove(id);
            applyStun(player);
        }

        if (stunnedPlayers.contains(id)) {
            event.setTo(event.getFrom());
        }
    }

    @Override
    public String getName() {
        return ChatColor.GRAY + "Metal Dash";
    }

    @Override
    public String getDescription() {
        return "Dash forward, damaging enemies. Missing stuns you.";
    }
}
