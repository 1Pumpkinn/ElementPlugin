package saturn.elementPlugin.commands;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class ElementInfoCommand implements CommandExecutor, TabCompleter {

    private final Map<ElementType, ElementInfo> elementInfoMap;

    public ElementInfoCommand(ElementPlugin plugin) {
        this.elementInfoMap = initializeElementInfo();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /elements <element>")
                    .color(NamedTextColor.YELLOW));
            return true;
        }

        showElementDetails(player, args[0]);
        return true;
    }

    /**
     * Show detailed information about a specific element
     */
    private void showElementDetails(Player player, String elementName) {
        ElementType type;
        try {
            type = ElementType.valueOf(elementName.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("❌ Unknown element: " + elementName)
                    .color(NamedTextColor.RED));
            player.sendMessage(Component.text("Use /elements <element>")
                    .color(NamedTextColor.GRAY));
            return;
        }

        ElementInfo info = elementInfoMap.get(type);
        if (info == null) {
            player.sendMessage(Component.text("❌ No information available for " + type.name())
                    .color(NamedTextColor.RED));
            return;
        }

        // Header
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text("✦ " + type.name() + " ELEMENT ✦")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.empty());

        // Description - only show if not empty
        if (info.description != null && !info.description.isEmpty()) {
            player.sendMessage(Component.text("📖 " + info.description)
                    .color(NamedTextColor.GRAY));
            player.sendMessage(Component.empty());
        }

        // Passive Benefits
        player.sendMessage(Component.text("⭐ Passive Benefits:")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));
        for (String upside : info.upsides) {
            player.sendMessage(Component.text("  • " + upside)
                    .color(NamedTextColor.GREEN));
        }
        player.sendMessage(Component.empty());

        // Abilities
        player.sendMessage(Component.text("⚡ Abilities:")
                .color(NamedTextColor.GOLD)
                .decorate(TextDecoration.BOLD));

        player.sendMessage(Component.text("  ① " + info.ability1Name)
                .color(NamedTextColor.AQUA)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("     " + info.ability1Desc)
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("     Mana: " + info.ability1Cost)
                .color(NamedTextColor.YELLOW));

        player.sendMessage(Component.text("  ② " + info.ability2Name)
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text("     " + info.ability2Desc)
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("     Mana: " + info.ability2Cost)
                .color(NamedTextColor.YELLOW));

        player.sendMessage(Component.empty());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();

            for (ElementType type : ElementType.values()) {
                String name = type.name().toLowerCase();
                if (name.startsWith(input)) {
                    completions.add(name);
                }
            }
            return completions;
        }
        return Collections.emptyList();
    }

    /**
     * Initialize all element information
     * UPDATED: Removed descriptive sentences, Death abilities swapped
     */
    private Map<ElementType, ElementInfo> initializeElementInfo() {
        Map<ElementType, ElementInfo> map = new EnumMap<>(ElementType.class);

        map.put(ElementType.WATER, new ElementInfo(
                "",
                Arrays.asList(
                        "Conduit Power (underwater breathing)",
                        "Mine faster underwater (Upgrade II)"
                ),
                "Water Whirlpool", "Create a spinning vortex", 50,
                "Water Prison", "Trap an enemy in a sphere of water", 75
        ));

        map.put(ElementType.FIRE, new ElementInfo(
                "",
                Arrays.asList(
                        "Fire Resistance (immune to fire/lava)",
                        "Fire Aspect on all attacks (Upgrade II)"
                ),
                "Hellish Flames", "Set enemies ablaze with inextinguishable flames for 10s", 50,
                "Phoenix Form", "Revive from death with 1 HP, explode and become invincible for 3s (5min cooldown)", 75
        ));

        map.put(ElementType.EARTH, new ElementInfo(
                "",
                Arrays.asList(
                        "Hero of The Village",
                        "1.5x ore drops (Upgrade II)"
                ),
                "Earth Tunnel", "Dig tunnels through stone and dirt", 50,
                "Earthquake", "Create a powerful earthquake that stuns all enemies within 10 blocks for 3 seconds", 75
        ));

        map.put(ElementType.AIR, new ElementInfo(
                "",
                Arrays.asList(
                        "No fall damage",
                        "Immunity to powdered snow, soul sand, and slow blocks"
                ),
                "Air Blast", "Push enemies away with a gust of wind", 50,
                "Air Dash", "Dash forward swiftly, pushing enemies aside", 75
        ));

        map.put(ElementType.FROST, new ElementInfo(
                "",
                Arrays.asList(
                        "Speed III on ice",
                        "Freeze on hit (10% chance, Upgrade II)"
                ),
                "Ice Shard Volley", "Fire 5 ice shards in a cone", 75,
                "Frost Nova", "Create an explosion of ice around you", 50
        ));

        map.put(ElementType.METAL, new ElementInfo(
                "",
                Arrays.asList(
                        "Haste I",
                        "Take 50% less knockback (Upgrade II)"
                ),
                "Metal Dash", "Dash forward, damaging enemies", 50,
                "Chain Reel", "Pull an enemy toward you", 75
        ));

        map.put(ElementType.LIFE, new ElementInfo(
                "",
                Arrays.asList(
                        "Slower hunger drain (15% slower)",
                        "15 hearts total"
                ),
                "Regeneration Aura", "Heals you and allies around you", 50,
                "Healing Beam", "Heal an ally directly", 75
        ));

        // SWAPPED: Death Slash is ability 1, Death Clock is ability 2
        map.put(ElementType.DEATH, new ElementInfo(
                "",
                Arrays.asList(
                        "25% more XP from kills",
                        "Wither on hit (10% chance, Upgrade II)"
                ),
                "Death Slash", "Your next hit causes bleeding for 5 seconds", 50,
                "Death Clock", "Your next hit curses with blindness, weakness, and wither", 75
        ));

        return map;
    }

    private static class ElementInfo {
        final String description;
        final List<String> upsides;
        final String ability1Name;
        final String ability1Desc;
        final int ability1Cost;
        final String ability2Name;
        final String ability2Desc;
        final int ability2Cost;

        ElementInfo(String description, List<String> upsides,
                    String ability1Name, String ability1Desc, int ability1Cost,
                    String ability2Name, String ability2Desc, int ability2Cost) {
            this.description = description;
            this.upsides = upsides;
            this.ability1Name = ability1Name;
            this.ability1Desc = ability1Desc;
            this.ability1Cost = ability1Cost;
            this.ability2Name = ability2Name;
            this.ability2Desc = ability2Desc;
            this.ability2Cost = ability2Cost;
        }
    }
}