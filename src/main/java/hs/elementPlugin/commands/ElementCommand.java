package hs.elementPlugin.commands;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ElementCommand implements CommandExecutor {
    private final ElementManager elements;

    public ElementCommand(ElementManager elements) {
        this.elements = elements;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /element <reroll|set> [player] <air|water|fire|earth|life>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reroll" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only"); return true; }
                elements.rollAndAssign(p);
            }
            case "set" -> {
                if (args.length == 2 && sender instanceof Player p) {
                    ElementType type = parse(args[1]);
                    if (type == null) { sender.sendMessage(ChatColor.RED + "Invalid element"); return true; }
                    elements.setElement(p, type);
                } else if (args.length >= 3) {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) { sender.sendMessage(ChatColor.RED + "Player not found"); return true; }
                    ElementType type = parse(args[2]);
                    if (type == null) { sender.sendMessage(ChatColor.RED + "Invalid element"); return true; }
                    elements.setElement(target, type);
                    sender.sendMessage(ChatColor.GREEN + "Set element for " + target.getName() + " to " + type.name());
                } else {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /element set [player] <air|water|fire|earth|life>");
                }
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /element <reroll|set> [player] <air|water|fire|earth|life>");
        }
        return true;
    }

    private ElementType parse(String s) {
        try { return ElementType.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }
}