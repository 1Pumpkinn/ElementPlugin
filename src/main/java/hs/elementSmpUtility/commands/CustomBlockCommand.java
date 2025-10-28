package hs.elementSmpUtility.commands;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import hs.elementSmpUtility.blocks.CustomBlockType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Command to give players custom blocks
 */
public class CustomBlockCommand implements CommandExecutor, TabCompleter {

    private final CustomBlockManager blockManager;

    public CustomBlockCommand(CustomBlockManager blockManager) {
        this.blockManager = blockManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command!")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        if (!player.hasPermission("elementsmp.customblock")) {
            player.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(TextColor.color(0xFF5555)));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give":
                handleGive(player, args);
                break;

            case "list":
                handleList(player);
                break;

            default:
                sendUsage(player);
                break;
        }

        return true;
    }

    private void handleGive(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /customblock give <block_id> [amount]")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        String blockId = args[1];
        int amount = 1;

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
                if (amount < 1 || amount > 64) {
                    player.sendMessage(Component.text("Amount must be between 1 and 64!")
                            .color(TextColor.color(0xFF5555)));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Invalid amount!")
                        .color(TextColor.color(0xFF5555)));
                return;
            }
        }

        ItemStack item = blockManager.createCustomBlock(blockId, amount);
        if (item == null) {
            player.sendMessage(Component.text("Unknown block type: " + blockId)
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        player.getInventory().addItem(item);
        player.sendMessage(Component.text("Given " + amount + "x ")
                .color(TextColor.color(0x55FF55))
                .append(blockManager.getBlockType(blockId).getDisplayName()));
    }

    private void handleList(Player player) {
        Map<String, CustomBlockType> blocks = blockManager.getAllBlockTypes();

        player.sendMessage(Component.text("=== Custom Blocks ===")
                .color(TextColor.color(0x55FFFF)));

        for (Map.Entry<String, CustomBlockType> entry : blocks.entrySet()) {
            CustomBlockType type = entry.getValue();
            player.sendMessage(
                    Component.text("• " + entry.getKey())
                            .color(TextColor.color(0xFFFFFF))
                            .append(Component.text(" - "))
                            .append(type.getDisplayName())
            );
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("=== Custom Block Commands ===")
                .color(TextColor.color(0x55FFFF)));
        player.sendMessage(Component.text("/customblock give <block_id> [amount]")
                .color(TextColor.color(0xFFFFFF)));
        player.sendMessage(Component.text("/customblock list")
                .color(TextColor.color(0xFFFFFF)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("give");
            completions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            completions.addAll(blockManager.getAllBlockTypes().keySet());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.add("1");
            completions.add("16");
            completions.add("32");
            completions.add("64");
        }

        return completions;
    }
}