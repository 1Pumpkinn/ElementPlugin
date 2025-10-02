package hs.elementPlugin.commands;

import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ManaCommand implements CommandExecutor {
    private final ManaManager manaManager;
    private final ConfigManager configManager;

    public ManaCommand(ManaManager manaManager, ConfigManager configManager) {
        this.manaManager = manaManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mana <reset|set> [player] [amount]");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reset" -> {
                if (args.length == 1 && sender instanceof Player p) {
                    // Reset own mana
                    int maxMana = configManager.getMaxMana();
                    var pd = manaManager.get(p.getUniqueId());
                    pd.setMana(maxMana);
                    p.sendMessage(ChatColor.GREEN + "Your mana has been reset to " + maxMana);
                } else if (args.length >= 2) {
                    // Reset target's mana
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return true;
                    }
                    int maxMana = configManager.getMaxMana();
                    var pd = manaManager.get(target.getUniqueId());
                    pd.setMana(maxMana);
                    sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s mana to " + maxMana);
                    target.sendMessage(ChatColor.GREEN + "Your mana has been reset to " + maxMana);
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /mana reset [player]");
                }
            }
            case "set" -> {
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /mana set <player> <amount>");
                    return true;
                }

                Player target;
                int amount;

                if (args.length == 2 && sender instanceof Player) {
                    // /mana set <amount> - set own mana
                    target = (Player) sender;
                    try {
                        amount = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid amount");
                        return true;
                    }
                } else if (args.length >= 3) {
                    // /mana set <player> <amount>
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(ChatColor.RED + "Player not found");
                        return true;
                    }
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.RED + "Invalid amount");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /mana set <player> <amount>");
                    return true;
                }

                var pd = manaManager.get(target.getUniqueId());
                pd.setMana(amount);
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s mana to " + amount);
                if (!target.equals(sender)) {
                    target.sendMessage(ChatColor.GREEN + "Your mana has been set to " + amount);
                }
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /mana <reset|set> [player] [amount]");
        }
        return true;
    }
}