package hs.elementPlugin.util;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class DamageTester implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        // Check if player is OP
        if (!player.isOp()) {
            player.sendMessage("§cYou must be an operator to use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /damagetest <spawn>");
            return true;
        }

        player.sendMessage("§cUnknown subcommand. Usage: /damagetest <spawn>");
        return true;
    }

    private void spawnTestVillager(Player player) {
        // Spawn villager at player's location
        Villager villager = (Villager) player.getWorld().spawnEntity(
                player.getLocation(),
                EntityType.VILLAGER
        );

        // Disable AI
        villager.setAI(false);
        villager.setGravity(true); // Keep gravity so it doesn't float
        villager.setSilent(true); // No sounds
        villager.setInvulnerable(false); // Make sure it can take damage

        // Create PROTECTION IV armor
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        helmet.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        chestplate.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        leggings.addEnchantment(Enchantment.PROTECTION, 4);

        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        boots.addEnchantment(Enchantment.PROTECTION, 4);

        // Equip the armor
        EntityEquipment equipment = villager.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(helmet);
            equipment.setChestplate(chestplate);
            equipment.setLeggings(leggings);
            equipment.setBoots(boots);

            // Prevent armor from dropping
            equipment.setHelmetDropChance(0.0f);
            equipment.setChestplateDropChance(0.0f);
            equipment.setLeggingsDropChance(0.0f);
            equipment.setBootsDropChance(0.0f);
        }

        // Set custom name for identification
        villager.setCustomName("§6Test Dummy");
        villager.setCustomNameVisible(true);

        player.sendMessage("§aSpawned test villager with PROTECTION IV armor!");
    }
}