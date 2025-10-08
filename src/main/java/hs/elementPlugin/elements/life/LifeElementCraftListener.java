package hs.elementPlugin.elements.life;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LifeElementCraftListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;


    public LifeElementCraftListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }


    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        
        ItemStack result = e.getRecipe() == null ? null : e.getRecipe().getResult();
        if (result == null) return;
        
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        Byte isElem = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE);
        if (isElem == null || isElem != (byte)1) return;
        
        String elementType = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
        if (!"LIFE".equals(elementType)) return;
        
        PlayerData pd = elementManager.data(p.getUniqueId());

        if (plugin.getDataStore().isLifeElementCrafted()) {
            e.setCancelled(true);
            return;
        }
        
        if (pd.hasElementItem(ElementType.LIFE)) {
            e.setCancelled(true);
            return;
        }

        consumeIngredients(e);
        
        e.setCancelled(true);
        p.getInventory().addItem(result);
        
        pd.addElementItem(ElementType.LIFE);
        pd.setCurrentElementUpgradeLevel(0);
        plugin.getDataStore().setLifeElementCrafted(true);
        plugin.getDataStore().save(pd);
        
        p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);
        
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + "🌟 " + p.getName() + " has crafted the Life Element! 🌟");
    }


    private void consumeIngredients(CraftItemEvent e) {
        CraftingInventory craftingInv = e.getInventory();
        ItemStack[] matrix = craftingInv.getMatrix();
        org.bukkit.inventory.Recipe recipe = e.getRecipe();
        
        if (recipe instanceof org.bukkit.inventory.ShapedRecipe shapedRecipe) {
            String[] shape = shapedRecipe.getShape();
            java.util.Map<Character, org.bukkit.inventory.RecipeChoice> ingredients = shapedRecipe.getChoiceMap();
            
            for (int i = 0; i < matrix.length; i++) {
                ItemStack item = matrix[i];
                if (item == null || item.getType() == Material.AIR) continue;
                
                int row = i / 3;
                int col = i % 3;
                boolean isPartOfRecipe = false;
                
                if (row < shape.length && col < shape[row].length()) {
                    char ingredientChar = shape[row].charAt(col);
                    isPartOfRecipe = ingredients.containsKey(ingredientChar);
                }
                
                if (isPartOfRecipe) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                        matrix[i] = item;
                    } else {
                        matrix[i] = null;
                    }
                }
            }
        } else {
            for (int i = 0; i < matrix.length; i++) {
                if (matrix[i] != null && matrix[i].getType() != Material.AIR) {
                    if (matrix[i].getAmount() > 1) {
                        matrix[i].setAmount(matrix[i].getAmount() - 1);
                    } else {
                        matrix[i] = null;
                    }
                }
            }
        }
        
        craftingInv.setMatrix(matrix);
    }
}
