package hs.elementPlugin.elements.abilities;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all abilities in the plugin
 */
public class AbilityManager {
    private final ElementPlugin plugin;
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<ElementType, Map<Integer, Ability>> elementAbilities = new HashMap<>();

    public AbilityManager(ElementPlugin plugin) {
        this.plugin = plugin;

        // Initialize maps for each element type
        for (ElementType type : ElementType.values()) {
            elementAbilities.put(type, new HashMap<>());
        }
    }

    /**
     * Register an ability
     *
     * @param elementType The element type this ability belongs to
     * @param abilityNumber The ability number (1 or 2)
     * @param ability The ability to register
     */
    public void registerAbility(ElementType elementType, int abilityNumber, Ability ability) {
        abilities.put(ability.getAbilityId(), ability);
        elementAbilities.get(elementType).put(abilityNumber, ability);
    }

    /**
     * Execute an ability for a player (NO COOLDOWN CHECK)
     *
     * @param context The element context
     * @param abilityNumber The ability number (1 or 2)
     * @return true if the ability was executed successfully, false otherwise
     */
    public boolean executeAbility(ElementContext context, int abilityNumber) {
        Player player = context.getPlayer();
        ElementType elementType = context.getElementType();

        // Get the ability
        Ability ability = elementAbilities.get(elementType).get(abilityNumber);
        if (ability == null) {
            player.sendMessage(ChatColor.RED + "This ability doesn't exist!");
            return false;
        }

        // Check upgrade level
        if (context.getUpgradeLevel() < ability.getRequiredUpgradeLevel()) {
            player.sendMessage(ChatColor.RED + "You need Upgrade " +
                    (ability.getRequiredUpgradeLevel() == 1 ? "I" : "II") + " to use this ability.");
            return false;
        }

        // Check if ability is already active
        if (ability.isActiveFor(player)) {
            player.sendMessage(ChatColor.YELLOW + "This ability is already active!");
            return false;
        }

        // Check mana (NO COOLDOWN CHECK)
        int cost = ability.getManaCost();
        if (context.getManaManager().get(player.getUniqueId()).getMana() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }

        // Execute ability
        if (ability.execute(context)) {
            context.getManaManager().spend(player, cost);
            return true;
        }

        return false;
    }

    /**
     * Get an ability by its ID
     *
     * @param abilityId The ability ID
     * @return The ability, or null if not found
     */
    public Ability getAbility(String abilityId) {
        return abilities.get(abilityId);
    }
}