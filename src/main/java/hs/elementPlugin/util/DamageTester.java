package hs.elementPlugin.util;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class DamageTester implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.isOp()) {
            player.sendMessage("§cYou must be an operator to use this command.");
            return true;
        }

        // === Correct usage handling ===
        if (args.length == 0) {
            player.sendMessage("§cUsage: /damagetest spawn");
            return true;
        }

        if (args[0].equalsIgnoreCase("spawn")) {
            spawnTestZombie(player);
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Usage: /damagetest spawn");
        return true;
    }

    private void spawnTestZombie(Player player) {
        Zombie zombie = (Zombie) player.getWorld().spawnEntity(
                player.getLocation(),
                EntityType.ZOMBIE
        );

        zombie.setAI(true);
        zombie.setGravity(true);
        zombie.setSilent(true);
        zombie.setInvulnerable(false);

        // Armor
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 4);

        EntityEquipment equipment = zombie.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(helmet);
            equipment.setChestplate(chestplate);
            equipment.setLeggings(leggings);
            equipment.setBoots(boots);

            equipment.setHelmetDropChance(0f);
            equipment.setChestplateDropChance(0f);
            equipment.setLeggingsDropChance(0f);
            equipment.setBootsDropChance(0f);
        }

        zombie.setCustomName("§6Test Dummy");
        zombie.setCustomNameVisible(true);

        player.sendMessage("§aSpawned test zombie with PROTECTION IV armor!");
    }
}