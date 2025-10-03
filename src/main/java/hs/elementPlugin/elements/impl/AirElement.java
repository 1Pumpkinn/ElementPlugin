package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirElement extends BaseElement {

    public AirElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.AIR;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanent speed 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

        // Animated particle ring that shoots outward
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                double currentRadius = 1.5 + (tick * 0.8);
                if (currentRadius > 8.0) {
                    cancel();
                    return;
                }

                // Spawn particles in a ring above ground
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    // Ensure particles spawn above ground level
                    Location particleLoc = center.clone().add(x, 1.0, z);
                    // Check if the location is solid, if so move particles up
                    while (particleLoc.getBlock().getType().isSolid() && particleLoc.getY() < center.getY() + 5) {
                        particleLoc.add(0, 1, 0);
                    }

                    int count = Math.max(1, 3 - tick/2);
                    w.spawnParticle(Particle.CLOUD, particleLoc, count, 0.0, 0.0, 0.0, 0.0);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Launch nearby entities
        for (LivingEntity e : player.getLocation().getNearbyLivingEntities(radius)) {
            if (!isValidTarget(context, e)) continue;
            Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.25).setY(1.5);
            e.setVelocity(push);
        }
        w.playSound(center, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        
        // Mark ability as active to prevent elytra removal
        setAbility2Active(player, true);
        
        // Launch player 30 blocks up first
        Vector upward = new Vector(0, 2.0, 0);
        player.setVelocity(upward);
        
        // Store original chestplate
        ItemStack originalChestplate = player.getInventory().getChestplate();
        
        // Wait a moment for the launch, then give elytra
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isAbility2Active(player)) {
                    // Give elytra with curse of binding
                    ItemStack elytra = new ItemStack(Material.ELYTRA);
                    elytra.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BINDING_CURSE, 1);
                    player.getInventory().setChestplate(elytra);
                    player.sendMessage(ChatColor.AQUA + "Elytra activated! Glide for 10 seconds!");
                    
                    // Remove elytra after 10 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.getInventory().setChestplate(originalChestplate);
                                setAbility2Active(player, false);
                                player.sendMessage(ChatColor.GRAY + "Elytra effect ended.");
                            }
                        }
                    }.runTaskLater(plugin, 200L); // 10 seconds = 200 ticks
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay after launch
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        player.sendMessage(ChatColor.AQUA + "Launching into the sky!");
        return true;
    }
}