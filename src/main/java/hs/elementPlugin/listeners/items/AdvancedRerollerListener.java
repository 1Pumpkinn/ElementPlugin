package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AdvancedRerollerListener implements Listener {
    private final ElementPlugin plugin;
    private final Random random = new Random();

    private static final ElementType[] ADVANCED_ELEMENTS = {
            ElementType.METAL,
            ElementType.FROST,
            ElementType.LIFE,
            ElementType.DEATH
    };

    public AdvancedRerollerListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancedRerollerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        var container = meta.getPersistentDataContainer();

        if (!container.has(ItemKeys.advancedReroller(plugin), PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // CRITICAL FIX: Prevent offhand usage
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
            return;
        }

        // CRITICAL FIX: Always cancel the event to prevent spam
        event.setCancelled(true);

        var elementManager = plugin.getElementManager();
        if (elementManager.isCurrentlyRolling(player)) {
            // Don't send message on every spam click - just silently ignore
            return;
        }

        // CRITICAL FIX: Mark player as rolling BEFORE consuming item
        elementManager.setCurrentlyRolling(player, true);

        // CRITICAL FIX: Clear ALL element effects BEFORE starting roll
        clearAllElementEffects(player);

        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType current = pd.getCurrentElement();
        ElementType newElement = determineNewElement(current);

        // Consume one reroller AFTER marking as rolling
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) player.getInventory().removeItem(item);

        performAdvancedRoll(player, newElement);
    }

    private ElementType determineNewElement(ElementType current) {
        List<ElementType> availableElements = new ArrayList<>();

        for (ElementType element : ADVANCED_ELEMENTS) {
            if (element != current) {
                availableElements.add(element);
            }
        }

        if (availableElements.isEmpty()) {
            return ADVANCED_ELEMENTS[random.nextInt(ADVANCED_ELEMENTS.length)];
        }

        return availableElements.get(random.nextInt(availableElements.size()));
    }

    private void performAdvancedRoll(Player player, ElementType targetElement) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        String[] names = {"METAL", "FROST", "LIFE", "DEATH"};
        int steps = 20;
        long interval = 3L;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                // CRITICAL: Check if player is still online during animation
                if (!player.isOnline()) {
                    plugin.getElementManager().cancelRolling(player);
                    cancel();
                    return;
                }

                if (tick >= steps) {
                    assignAdvancedElement(player, targetElement);
                    cancel();
                    return;
                }

                String name = names[tick % 4];
                player.sendTitle(
                        ChatColor.GOLD + "Rolling...",
                        ChatColor.AQUA + name,
                        0, 10, 0
                );
                tick++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void assignAdvancedElement(Player player, ElementType element) {
        // Double-check player is still online
        if (!player.isOnline()) {
            plugin.getElementManager().cancelRolling(player);
            return;
        }

        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());

        // Preserve upgrade level
        int currentUpgradeLevel = pd.getCurrentElementUpgradeLevel();
        pd.setCurrentElementWithoutReset(element);
        pd.setCurrentElementUpgradeLevel(currentUpgradeLevel);
        plugin.getDataStore().save(pd);

        var title = net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("Element Chosen!")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(element.name())
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(2000),
                        java.time.Duration.ofMillis(500)
                )
        );

        player.showTitle(title);

        // Apply new element effects
        plugin.getElementManager().applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        player.sendMessage(ChatColor.GREEN + "Your element has been rerolled to " +
                ChatColor.AQUA + element.name() + ChatColor.GREEN + "!");

        // Mark rolling as complete
        plugin.getElementManager().cancelRolling(player);
    }

    /**
     * CRITICAL: Clear ALL element effects to prevent stacking
     */
    private void clearAllElementEffects(Player player) {
        // Clear effects from EVERY element type
        for (ElementType type : ElementType.values()) {
            Element element = plugin.getElementManager().get(type);
            if (element != null) {
                element.clearEffects(player);
            }
        }

        // Reset max health to default (20 HP) while preserving current health
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != 20.0) {
            // Store current health before changing max health
            double currentHealth = player.getHealth();
            attr.setBaseValue(20.0);

            // Restore current health (capped at new max if necessary)
            if (!player.isDead() && currentHealth > 0) {
                player.setHealth(Math.min(currentHealth, 20.0));
            }
        }

        plugin.getLogger().fine("Cleared all element effects for " + player.getName() + " before advanced reroll");
    }
}