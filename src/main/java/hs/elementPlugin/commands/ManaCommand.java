package hs.elementPlugin.commands;

import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ManaCommand implements CommandExecutor {
    private final ManaManager manaManager;
    private final ConfigManager configManager;
    private final Map<String, ManaOperation> operations;

    public ManaCommand(ManaManager manaManager, ConfigManager configManager) {
        this.manaManager = manaManager;
        this.configManager = configManager;
        this.operations = initializeOperations();
    }

    private Map<String, ManaOperation> initializeOperations() {
        Map<String, ManaOperation> ops = new HashMap<>();
        ops.put("reset", new ResetOperation());
        ops.put("set", new SetOperation());
        return ops;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mana <reset|set> [player] [amount]");
            return true;
        }

        ManaOperation operation = operations.get(args[0].toLowerCase());
        if (operation != null) {
            operation.execute(sender, args);
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /mana <reset|set> [player] [amount]");
        }

        return true;
    }

    private interface ManaOperation {
        void execute(CommandSender sender, String[] args);
    }

    private class ResetOperation implements ManaOperation {
        @Override
        public void execute(CommandSender sender, String[] args) {
            int maxMana = configManager.getMaxMana();

            Optional<Player> target = resolveTarget(sender, args, 1);
            if (target.isEmpty()) {
                sender.sendMessage(ChatColor.RED + "Player not found");
                return;
            }

            Player player = target.get();
            manaManager.get(player.getUniqueId()).setMana(maxMana);

            sendSuccessMessages(sender, player, "reset to " + maxMana);
        }
    }

    private class SetOperation implements ManaOperation {
        @Override
        public void execute(CommandSender sender, String[] args) {
            ManaSetContext context = parseManaSetArgs(sender, args);
            if (context == null) {
                sender.sendMessage(ChatColor.YELLOW + "Usage: /mana set <player> <amount>");
                return;
            }

            manaManager.get(context.target.getUniqueId()).setMana(context.amount);
            sendSuccessMessages(sender, context.target, "set to " + context.amount);
        }

        private ManaSetContext parseManaSetArgs(CommandSender sender, String[] args) {
            // /mana set <amount> (self)
            if (args.length == 2 && sender instanceof Player) {
                Optional<Integer> amount = parseAmount(args[1]);
                return amount.map(value -> new ManaSetContext((Player) sender, value)).orElse(null);
            }

            // /mana set <player> <amount>
            if (args.length >= 3) {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return null;

                Optional<Integer> amount = parseAmount(args[2]);
                return amount.map(value -> new ManaSetContext(target, value)).orElse(null);
            }

            return null;
        }
    }

    private Optional<Player> resolveTarget(CommandSender sender, String[] args, int argIndex) {
        if (args.length <= argIndex) {
            return sender instanceof Player ? Optional.of((Player) sender) : Optional.empty();
        }
        return Optional.ofNullable(Bukkit.getPlayer(args[argIndex]));
    }

    private Optional<Integer> parseAmount(String input) {
        try {
            int amount = Integer.parseInt(input);
            return amount >= 0 ? Optional.of(amount) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void sendSuccessMessages(CommandSender sender, Player target, String action) {
        sender.sendMessage(ChatColor.GREEN + "Mana " + action + " for " + target.getName());
        if (!target.equals(sender)) {
            target.sendMessage(ChatColor.GREEN + "Your mana has been " + action);
        }
    }

    private static class ManaSetContext {
        final Player target;
        final int amount;

        ManaSetContext(Player target, int amount) {
            this.target = target;
            this.amount = amount;
        }
    }
}