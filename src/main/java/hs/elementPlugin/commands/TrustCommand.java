package hs.elementPlugin.commands;

import hs.elementPlugin.managers.TrustManager;
import hs.elementPlugin.ElementPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class TrustCommand implements CommandExecutor {
    private final ElementPlugin plugin;
    private final TrustManager trust;

    public TrustCommand(ElementPlugin plugin, TrustManager trust) {
        this.plugin = plugin;
        this.trust = trust;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only");
            return true;
        }
        if (args.length == 0) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /trust <list|add|remove> [player]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> {
                var names = trust.getTrustedNames(p.getUniqueId());
                p.sendMessage(ChatColor.AQUA + "Trusted: " + ChatColor.WHITE + (names.isEmpty() ? "(none)" : String.join(", ", names)));
            }
            case "add" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust add <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                if (target.equals(p)) { p.sendMessage(ChatColor.RED + "You cannot trust yourself"); return true; }
                if (trust.isTrusted(p.getUniqueId(), target.getUniqueId()) && trust.isTrusted(target.getUniqueId(), p.getUniqueId())) {
                    p.sendMessage(ChatColor.YELLOW + "You are already mutually trusted.");
                    return true;
                }
                trust.addPending(target.getUniqueId(), p.getUniqueId());
                // Send clickable message to target
                Component msg = Component.text(p.getName() + " wants to trust with you. ", NamedTextColor.GOLD)
                        .append(Component.text("[ACCEPT]", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/trust accept " + p.getUniqueId())))
                        .append(Component.text(" "))
                        .append(Component.text("[DENY]", NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/trust deny " + p.getUniqueId())));
                target.sendMessage(msg);
                p.sendMessage(ChatColor.GREEN + "Sent trust request to " + target.getName());
            }
            case "accept" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust accept <player|uuid>"); return true; }
                Player from = Bukkit.getPlayer(args[1]);
                UUID fromId = null;
                if (from != null) fromId = from.getUniqueId();
                else {
                    try { fromId = UUID.fromString(args[1]); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                }
                if (!trust.hasPending(p.getUniqueId(), fromId)) { p.sendMessage(ChatColor.YELLOW + "No pending request from that player."); return true; }
                trust.clearPending(p.getUniqueId(), fromId);
                trust.addMutualTrust(p.getUniqueId(), fromId);
                p.sendMessage(ChatColor.GREEN + "You are now mutually trusted.");
                Player other = Bukkit.getPlayer(fromId);
                if (other != null) other.sendMessage(ChatColor.GREEN + p.getName() + " accepted your trust request.");
            }
            case "deny" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust deny <player|uuid>"); return true; }
                Player from = Bukkit.getPlayer(args[1]);
                UUID fromId = null;
                if (from != null) fromId = from.getUniqueId();
                else {
                    try { fromId = UUID.fromString(args[1]); } catch (Exception ex) { p.sendMessage(ChatColor.RED + "Player not found"); return true; }
                }
                if (trust.hasPending(p.getUniqueId(), fromId)) {
                    trust.clearPending(p.getUniqueId(), fromId);
                    p.sendMessage(ChatColor.YELLOW + "Denied trust request.");
                    Player other = Bukkit.getPlayer(fromId);
                    if (other != null) other.sendMessage(ChatColor.RED + p.getName() + " denied your trust request.");
                } else {
                    p.sendMessage(ChatColor.YELLOW + "No pending request from that player.");
                }
            }
            case "remove" -> {
                if (args.length < 2) { p.sendMessage(ChatColor.RED + "Usage: /trust remove <player>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                UUID uuid;
                if (target != null) uuid = target.getUniqueId(); else {
                    // Fallback: try parsing UUID
                    try { uuid = UUID.fromString(args[1]); } catch (IllegalArgumentException ex) {
                        p.sendMessage(ChatColor.RED + "Player must be online or provide UUID");
                        return true;
                    }
                }
                trust.removeTrust(p.getUniqueId(), uuid);
                p.sendMessage(ChatColor.YELLOW + "Removed trust.");
            }
            default -> p.sendMessage(ChatColor.YELLOW + "Usage: /trust <list|add|remove> [player]");
        }
        return true;
    }
}