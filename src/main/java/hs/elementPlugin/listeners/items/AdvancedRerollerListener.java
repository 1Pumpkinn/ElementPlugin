package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class AdvancedRerollerListener implements Listener {
    private final ElementPlugin plugin;
    private final Random random = new Random();

    public AdvancedRerollerListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancedRerollerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(ItemKeys.advancedReroller(plugin), PersistentDataType.BYTE)) {

            // Only activate on right-click, not left-click/hit or other actions
            org.bukkit.event.block.Action action = event.getAction();
            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                    action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            // CRITICAL: Check if clicking on a pedestal - if so, don't use the reroller
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                org.bukkit.block.Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null && clickedBlock.getType() == org.bukkit.Material.LODESTONE) {
                    hs.elementSmpUtility.storage.BlockDataStorage blockStorage = plugin.getBlockStorage();
                    String blockId = blockStorage.getCustomBlockIdCached(clickedBlock.getLocation());

                    if ("pedestal".equals(blockId)) {
                        return;
                    }
                }
            }

            event.setCancelled(true);

            // Check if player is already rolling
            if (plugin.getElementManager().isCurrentlyRolling(player)) {
                player.sendMessage(ChatColor.RED + "You are already rerolling your element!");
                return;
            }

            PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
            ElementType currentElement = pd.getCurrentElement();

            // Determine which element to roll
            ElementType newElement;

            if (currentElement == ElementType.METAL) {
                // If already Metal, can only roll Frost
                newElement = ElementType.FROST;
            } else if (currentElement == ElementType.FROST) {
                // If already Frost, can only roll Metal
                newElement = ElementType.METAL;
            } else {
                // Random choice between Metal and Frost
                newElement = random.nextBoolean() ? ElementType.METAL : ElementType.FROST;
            }

            // Remove one advanced reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // Perform the roll with animation
            performAdvancedRoll(player, newElement);
        }
    }

    private void performAdvancedRoll(Player player, ElementType targetElement) {
        // Add to rolling set
        plugin.getElementManager().data(player.getUniqueId());

        // Show rolling animation
        String[] names = {targetElement.name()};
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        int steps = 16;
        for (int i = 0; i < steps; i++) {
            int delay = i * 3;
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String name = names[random.nextInt(names.length)];
                ChatColor color = targetElement == ElementType.METAL ? ChatColor.GRAY : ChatColor.AQUA;
                player.sendTitle(color + "Rolling...", color + name, 0, 10, 0);
            }, delay);
        }

        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            assignAdvancedElement(player, targetElement);
        }, steps * 3L + 2L);
    }

    private void assignAdvancedElement(Player player, ElementType element) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());

        // Clear all effects from previous element
        clearAllEffects(player);

        // Store current upgrade level before changing element
        int currentUpgradeLevel = pd.getCurrentElementUpgradeLevel();

        // Set new element without resetting upgrade level
        pd.setCurrentElementWithoutReset(element);
        pd.setCurrentElementUpgradeLevel(currentUpgradeLevel);

        plugin.getDataStore().save(pd);

        // Show title
        ChatColor color = element == ElementType.METAL ? ChatColor.GRAY : ChatColor.AQUA;
        player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("Element Chosen!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(element.name()).color(
                        element == ElementType.METAL ?
                                net.kyori.adventure.text.format.NamedTextColor.GRAY :
                                net.kyori.adventure.text.format.NamedTextColor.AQUA
                ),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(2000),
                        java.time.Duration.ofMillis(500)
                )
        ));

        // Apply upsides for new element
        plugin.getElementManager().applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        player.sendMessage(ChatColor.GREEN + "Your element has been rerolled to " +
                color + element.name() + ChatColor.GREEN + "!");
    }

    private void clearAllEffects(Player player) {
        // Remove all potion effects
        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Reset max health to default (20 HP) ONLY if player has Life element
        // Life element gives 30 HP (15 hearts), so we need to reset it when switching
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        if (pd.getCurrentElement() == ElementType.LIFE) {
            var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(20.0);
                if (player.getHealth() > 20.0) player.setHealth(20.0);
            }
        }
    }
}