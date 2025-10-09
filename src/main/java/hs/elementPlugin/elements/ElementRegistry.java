package hs.elementPlugin.elements;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.AbilityManager;
import hs.elementPlugin.elements.impl.death.DeathElement;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry for all elements in the plugin
 */
public class ElementRegistry {
    private final ElementPlugin plugin;
    private final Map<ElementType, Supplier<Element>> elementSuppliers = new HashMap<>();
    private final AbilityManager abilityManager;
    
    public ElementRegistry(ElementPlugin plugin, AbilityManager abilityManager) {
        this.plugin = plugin;
        this.abilityManager = abilityManager;
        // Register Death element
        registerElement(ElementType.DEATH, () -> new DeathElement(plugin));
    }
    
    /**
     * Register an element
     * 
     * @param type The element type
     * @param supplier The supplier that creates the element
     */
    public void registerElement(ElementType type, Supplier<Element> supplier) {
        elementSuppliers.put(type, supplier);
    }
    
    /**
     * Register an ability for an element
     * 
     * @param type The element type
     * @param abilityNumber The ability number (1 or 2)
     * @param ability The ability
     */
    public void registerAbility(ElementType type, int abilityNumber, Ability ability) {
        abilityManager.registerAbility(type, abilityNumber, ability);
    }
    
    /**
     * Create an element
     * 
     * @param type The element type
     * @return The created element, or null if the type is not registered
     */
    public Element createElement(ElementType type) {
        Supplier<Element> supplier = elementSuppliers.get(type);
        if (supplier == null) {
            return null;
        }
        return supplier.get();
    }
    
    /**
     * Check if an element type is registered
     * 
     * @param type The element type
     * @return true if the element type is registered, false otherwise
     */
    public boolean isRegistered(ElementType type) {
        return elementSuppliers.containsKey(type);
    }
}