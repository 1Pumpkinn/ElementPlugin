package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.AdvancedRerollerItem;
import hs.elementPlugin.items.Upgrader1Item;
import hs.elementPlugin.items.Upgrader2Item;
import hs.elementPlugin.items.RerollerItem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UtilCommand implements CommandExecutor {
    private final ElementPlugin plugin;

    public UtilCommand(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("element.admin")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        // Create stacks of utility items
        ItemStack upgrader1Stack = Upgrader1Item.make(plugin);
        upgrader1Stack.setAmount(64);

        ItemStack upgrader2Stack = Upgrader2Item.make(plugin);
        upgrader2Stack.setAmount(64);

        ItemStack rerollerStack = RerollerItem.make(plugin);
        rerollerStack.setAmount(64);

        ItemStack advancedRerollerStack = AdvancedRerollerItem.make(plugin);
        advancedRerollerStack.setAmount(64);

        // Give items to player
        player.getInventory().addItem(upgrader1Stack, upgrader2Stack, rerollerStack, advancedRerollerStack);

        player.sendMessage(ChatColor.GREEN + "You have been given utility items!");
        player.sendMessage(ChatColor.YELLOW + "• 64x Upgrader I");
        player.sendMessage(ChatColor.YELLOW + "• 64x Upgrader II");
        player.sendMessage(ChatColor.YELLOW + "• 64x Reroller");
        player.sendMessage(ChatColor.DARK_PURPLE + "• 64x Advanced Reroller");

        return true;
    }
}