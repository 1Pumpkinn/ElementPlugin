package hs.elementPlugin.commands;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
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

        // Description
        player.sendMessage(Component.text("📖 " + info.description)
                .color(NamedTextColor.GRAY));
        player.sendMessage(Component.empty());

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
     */
    private Map<ElementType, ElementInfo> initializeElementInfo() {
        Map<ElementType, ElementInfo> map = new EnumMap<>(ElementType.class);

        map.put(ElementType.AIR, new ElementInfo(
                "Masters of the sky and wind",
                Arrays.asList("No fall damage", "5% chance to apply Slow Falling to enemies (Upgrade II)"),
                "Air Blast", "Push enemies away with a gust of wind", 50,
                "Air Dash", "Dash forward swiftly, pushing enemies aside", 75
        ));

        map.put(ElementType.FIRE, new ElementInfo(
                "Wielders of flame and destruction",
                Arrays.asList("Immune to fire/lava damage", "Apply Fire Aspect to all attacks (Upgrade II)"),
                "Fireball", "Launch an explosive fireball", 50,
                "Meteor Shower", "Rain down fireballs from the sky", 75
        ));

        map.put(ElementType.WATER, new ElementInfo(
                "Controllers of water and ocean currents",
                Arrays.asList("Infinite Water Breathing", "Conduit Power permanently"),
                "Water Geyser", "Launch enemies upward with water pressure", 75,
                "Water Beam", "Fire a damaging water beam", 50
        ));

        map.put(ElementType.EARTH, new ElementInfo(
                "Masters of stone and terrain",
                Arrays.asList("Hero of The Village", "Double ore drops (Upgrade II)"),
                "Earth Tunnel", "Dig tunnels through stone and dirt", 50,
                "Mob Charm", "Charm mobs to follow you", 75
        ));

        map.put(ElementType.LIFE, new ElementInfo(
                "Healers with power over vitality",
                Arrays.asList("15 hearts total", "Regeneration I", "Crops grow faster (Upgrade II)"),
                "Regeneration Aura", "Heals you and allies around you", 50,
                "Healing Beam", "Heal an ally directly", 75
        ));

        map.put(ElementType.DEATH, new ElementInfo(
                "Masters of decay and darkness",
                Arrays.asList("Permanent Night Vision", "Raw/undead foods heal you"),
                "Summon Undead", "Summon undead ally for 30s", 50,
                "Wither Skull", "Fire an explosive wither skull", 75
        ));

        map.put(ElementType.METAL, new ElementInfo(
                "Warriors of steel and chains",
                Arrays.asList("Haste I", "Arrow immunity (Upgrade II)"),
                "Chain Reel", "Pull an enemy toward you", 50,
                "Metal Dash", "Dash forward, damaging enemies", 75
        ));

        map.put(ElementType.FROST, new ElementInfo(
                "Controllers of ice and cold",
                Arrays.asList("Speed II on snow", "Speed III on ice (Upgrade II)"),
                "Freezing Circle", "Slow enemies around you", 50,
                "Frozen Punch", "Freeze an enemy for 5s", 75
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
