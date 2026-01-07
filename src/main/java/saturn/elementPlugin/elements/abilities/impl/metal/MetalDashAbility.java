package saturn.elementPlugin.elements.abilities.impl.metal;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MetalDashAbility extends BaseAbility implements Listener {

    private final ElementPlugin plugin;

    public static final String META_TRUE_DAMAGE = "TRUE_DAMAGE";

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
        UUID playerId = player.getUniqueId();

        Vector direction = player.getLocation().getDirection().normalize();
        Vector velocity = direction.multiply(2.5);
        velocity.setY(Math.max(velocity.getY(), 0.4));
        player.setVelocity(velocity);

        Set<UUID> hitEntities = new HashSet<>();

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_IRON_GOLEM_ATTACK,
                1.0f,
                1.5f
        );

        setActive(player, true);
        dashingPlayers.add(playerId);

        new BukkitRunnable() {
            int ticks = 0;
            boolean hitSomething = false;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 40) {
                    endDash(player, hitSomething);
                    cancel();
                    return;
                }

                Location loc = player.getLocation();

                player.getWorld().spawnParticle(
                        Particle.CRIT,
                        loc,
                        10,
                        0.3, 0.3, 0.3,
                        0.1
                );

                if (ticks % 2 == 0) {
                    for (LivingEntity entity : loc.getNearbyLivingEntities(2.5)) {
                        if (entity.equals(player)) continue;
                        if (entity instanceof ArmorStand) continue;
                        if (hitEntities.contains(entity.getUniqueId())) continue;
                        if (!saturn.elementPlugin.util.AbilityTrustValidator.canAffectTarget(plugin, player, entity, false)) continue;

                        // TRUE DAMAGE: 2 hearts (4.0 damage)
                        // CRITICAL: Set metadata with current timestamp RIGHT BEFORE damage call
                        entity.setMetadata(
                                META_TRUE_DAMAGE,
                                new FixedMetadataValue(plugin, System.currentTimeMillis())
                        );

                        // Deal damage - will be processed as true damage by TrueDamageListener
                        // This will bypass armor but STILL trigger totems
                        entity.damage(4.0, player);

                        hitEntities.add(entity.getUniqueId());
                        hitSomething = true;

                        Vector knockback = entity.getLocation().toVector()
                                .subtract(loc.toVector())
                                .normalize()
                                .setY(0.3);

                        entity.setVelocity(knockback.multiply(0.5));

                        entity.getWorld().playSound(
                                entity.getLocation(),
                                Sound.ENTITY_PLAYER_ATTACK_CRIT,
                                1.0f,
                                1.2f
                        );

                        plugin.getLogger().fine("Metal Dash hit " + entity.getName() +
                                " with true damage (4.0) - totems will trigger if fatal");
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    /**
     * Ends the dash naturally (timer finished)
     */
    private void endDash(Player player, boolean hitSomething) {
        UUID id = player.getUniqueId();

        dashingPlayers.remove(id);
        setActive(player, false);

        if (!hitSomething) {
            if (player.isOnGround()) {
                applyStun(player);
            } else {
                pendingStuns.put(id, true);
            }
        }
    }

    /**
     * HARD cancel — used for Totem pops
     */
    public void cancelDash(Player player) {
        UUID id = player.getUniqueId();

        dashingPlayers.remove(id);
        pendingStuns.remove(id);
        stunnedPlayers.remove(id);

        setActive(player, false);

        // Kill momentum instantly
        player.setVelocity(new Vector(0, 0, 0));
    }

    private void applyStun(Player player) {
        UUID id = player.getUniqueId();

        stunnedPlayers.add(id);
        pendingStuns.remove(id);

        player.sendMessage(ChatColor.GRAY + "You missed! Stunned for 5 seconds.");

        player.getWorld().spawnParticle(
                Particle.SMOKE,
                player.getLocation().add(0, 1, 0),
                30,
                0.3, 0.5, 0.3,
                0.05
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_IRON_GOLEM_DAMAGE,
                1.0f,
                0.8f
        );

        new BukkitRunnable() {
            @Override
            public void run() {
                stunnedPlayers.remove(id);
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.GREEN + "Stun ended.");
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (pendingStuns.containsKey(id) && player.isOnGround()) {
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
        return "Dash forward, dealing 2 hearts of true damage. Missing stuns you for 5 seconds.";
    }
}