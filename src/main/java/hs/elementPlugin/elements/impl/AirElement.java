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
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

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

                    Location particleLoc = center.clone().add(x, 1.0, z);
                    // Adjust particle height to appear above solid blocks
                    while (particleLoc.getBlock().getType().isSolid() && particleLoc.getY() < center.getY() + 5) {
                        particleLoc.add(0, 1, 0);
                    }

                    int count = Math.max(1, 3 - tick/2);
                    w.spawnParticle(Particle.CLOUD, particleLoc, count, 0.0, 0.0, 0.0, 0.0);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

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
        
        setAbility2Active(player, true);
        
        Vector upward = new Vector(0, 2.0, 0);
        player.setVelocity(upward);
        
        ItemStack originalChestplate = player.getInventory().getChestplate();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && isAbility2Active(player)) {
                    ItemStack elytra = new ItemStack(Material.ELYTRA);
                    elytra.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.BINDING_CURSE, 1);
                    elytra.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.VANISHING_CURSE, 1);
                    player.getInventory().setChestplate(elytra);
                    player.sendMessage(ChatColor.AQUA + "Elytra activated! Glide for 10 seconds!");
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.getInventory().setChestplate(originalChestplate);
                                setAbility2Active(player, false);
                                player.sendMessage(ChatColor.GRAY + "Elytra effect ended.");
                            }
                        }
                    }.runTaskLater(plugin, 200L); // 10 seconds
                }
            }
        }.runTaskLater(plugin, 20L); // 1 second delay after launch
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        player.sendMessage(ChatColor.AQUA + "Launching into the sky!");
        return true;
    }
}