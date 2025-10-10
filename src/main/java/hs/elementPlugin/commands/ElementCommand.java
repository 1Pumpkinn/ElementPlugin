package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ElementCommand implements CommandExecutor {
    private final ElementPlugin plugin;
    private final DataStore dataStore;
    private final ElementManager elementManager;

    public ElementCommand(ElementPlugin plugin) {
        this.plugin = plugin;
        this.dataStore = plugin.getDataStore();
        this.elementManager = plugin.getElementManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("element.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "disable":
                return handleDisable(sender, args);
            case "enable":
                return handleEnable(sender, args);
            case "status":
                return handleStatus(sender);
            case "set":
                return handleSet(sender, args);
            case "testcrops":
                return handleTestCrops(sender, args);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleDisable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /element disable <life|death>");
            return true;
        }

        String element = args[1].toLowerCase();
        switch (element) {
            case "life":
                dataStore.setLifeElementCrafted(true);
                sender.sendMessage(ChatColor.GREEN + "Life element recipes have been disabled.");
                break;
            case "death":
                dataStore.setDeathElementCrafted(true);
                sender.sendMessage(ChatColor.GREEN + "Death element recipes have been disabled.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid element. Use 'life' or 'death'.");
                return true;
        }
        return true;
    }

    private boolean handleEnable(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /element enable <life|death>");
            return true;
        }

        String element = args[1].toLowerCase();
        switch (element) {
            case "life":
                dataStore.setLifeElementCrafted(false);
                sender.sendMessage(ChatColor.GREEN + "Life element recipes have been enabled.");
                break;
            case "death":
                dataStore.setDeathElementCrafted(false);
                sender.sendMessage(ChatColor.GREEN + "Death element recipes have been enabled.");
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid element. Use 'life' or 'death'.");
                return true;
        }
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /element set <player> <air|water|fire|earth|life|death>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + args[1] + "' not found.");
            return true;
        }

        ElementType elementType;
        try {
            elementType = ElementType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid element type. Use: air, water, fire, earth, life, or death");
            return true;
        }

        // Check server-wide restrictions for Life and Death
        if (elementType == ElementType.LIFE && dataStore.isLifeElementCrafted()) {
            sender.sendMessage(ChatColor.RED + "Life element recipes are disabled. Enable them first with /element enable life");
            return true;
        }
        if (elementType == ElementType.DEATH && dataStore.isDeathElementCrafted()) {
            sender.sendMessage(ChatColor.RED + "Death element recipes are disabled. Enable them first with /element enable death");
            return true;
        }

        elementManager.setElement(target, elementType);
        sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s element to " + ChatColor.AQUA + elementType.name());
        target.sendMessage(ChatColor.GREEN + "Your element has been set to " + ChatColor.AQUA + elementType.name() + ChatColor.GREEN + " by an admin.");
        
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        boolean lifeCrafted = dataStore.isLifeElementCrafted();
        boolean deathCrafted = dataStore.isDeathElementCrafted();
        
        sender.sendMessage(ChatColor.GOLD + "=== Element Recipe Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Life Element: " + (lifeCrafted ? ChatColor.RED + "DISABLED" : ChatColor.GREEN + "ENABLED"));
        sender.sendMessage(ChatColor.YELLOW + "Death Element: " + (deathCrafted ? ChatColor.RED + "DISABLED" : ChatColor.GREEN + "ENABLED"));
        return true;
    }

    private boolean handleTestCrops(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }
        
        // Test crop growth around the player
        int cropsGrown = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    org.bukkit.block.Block block = player.getLocation().clone().add(dx, dy, dz).getBlock();
                    if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                        if (ageable.getAge() < ageable.getMaximumAge()) {
                            ageable.setAge(ageable.getMaximumAge());
                            block.setBlockData(ageable);
                            cropsGrown++;
                        }
                    }
                }
            }
        }
        
        sender.sendMessage(ChatColor.GREEN + "Tested crop growth: " + cropsGrown + " crops grown around you.");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Element Admin Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/element disable <life|death> - Disable element recipes");
        sender.sendMessage(ChatColor.YELLOW + "/element enable <life|death> - Enable element recipes");
        sender.sendMessage(ChatColor.YELLOW + "/element set <player> <element> - Set player's element");
        sender.sendMessage(ChatColor.YELLOW + "/element status - Check recipe status");
        sender.sendMessage(ChatColor.YELLOW + "/element testcrops - Test crop growth around you");
    }
}