package saturn.elementPlugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;

public class ElementCheckCommand implements CommandExecutor {

    private final ElementManager elementManager;

    public ElementCheckCommand(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        ElementType element = elementManager.getPlayerElement(player);

        if (element == null) {
            player.sendMessage(ChatColor.GRAY + "You don't have an element assigned.");
            return true;
        }

        player.sendMessage(
                ChatColor.YELLOW + "Your element is: " +
                        getFormattedElement(element)
        );

        return true;
    }

    private String getFormattedElement(ElementType element) {
        switch (element) {
            case FIRE:
                return ChatColor.RED + "Fire";
            case WATER:
                return ChatColor.AQUA + "Water";
            case EARTH:
                return ChatColor.GREEN + "Earth";
            case AIR:
                return ChatColor.WHITE + "Air";
            case LIFE:
                return ChatColor.GREEN + "Life";
            case DEATH:
                return ChatColor.DARK_PURPLE + "Death";
            case METAL:
                return ChatColor.GRAY + "Metal";
            case FROST:
                return ChatColor.AQUA + "Frost";
            default:
                return ChatColor.GOLD + element.name();
        }
    }
}
