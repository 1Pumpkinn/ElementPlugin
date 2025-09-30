package hs.elementPlugin.commands;

import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Ability1Command implements CommandExecutor {
    private final ElementManager elements;
    private final ManaManager mana;
    private final CooldownManager cooldowns;

    public Ability1Command(Object plugin, ElementManager elements, ManaManager mana, CooldownManager cooldowns) {
        this.elements = elements;
        this.mana = mana;
        this.cooldowns = cooldowns;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only"); return true; }
        boolean ok = elements.useAbility1(p);
        if (!ok) p.sendMessage(ChatColor.RED + "Ability failed.");
        return true;
    }
}