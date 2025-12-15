package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.util.SmartEffectCleaner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles basic reroller usage
 * Rerolls to basic elements: AIR, WATER, FIRE, EARTH
 * Clears ALL infinite element effects (including advanced element effects)
 */
public class RerollerListener implements Listener {

    private final ElementPlugin plugin;
    private final Random random = new Random();

    private static final ElementType[] BASIC_ELEMENTS = {
            ElementType.AIR,
            ElementType.WATER,
            ElementType.FIRE,
            ElementType.EARTH
    };

    public RerollerListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRerollerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || !item.hasItemMeta()) return;

        if (!item.getItemMeta().getPersistentDataContainer()
                .has(ItemKeys.reroller(plugin), PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getHand() != EquipmentSlot.HAND) return;
        event.setCancelled(true);

        var elementManager = plugin.getElementManager();
        if (elementManager.isCurrentlyRolling(player)) {
            return;
        }

        elementManager.setCurrentlyRolling(player, true);
        SmartEffectCleaner.clearForElementChange(plugin, player);

        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType current = pd.getCurrentElement();

        // GUARANTEED DIFFERENT: Get a new element that's different from current
        ElementType newElement = determineNewElement(current);

        // Consume one reroller AFTER marking as rolling
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) player.getInventory().removeItem(item);

        performBasicRoll(player, newElement);
    }

    /**
     * Determine a new element that is GUARANTEED to be different from current
     *
     * @param current The player's current element
     * @return A random basic element different from current
     */
    private ElementType determineNewElement(ElementType current) {
        // Always exclude the current element from options
        List<ElementType> availableElements = new ArrayList<>();

        for (ElementType element : BASIC_ELEMENTS) {
            if (element != current) {
                availableElements.add(element);
            }
        }

        if (availableElements.isEmpty()) {
            return BASIC_ELEMENTS[random.nextInt(BASIC_ELEMENTS.length)];
        }

        return availableElements.get(random.nextInt(availableElements.size()));
    }

    private void performBasicRoll(Player player, ElementType targetElement) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        String[] names = {"AIR", "WATER", "FIRE", "EARTH"};
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
                    assignBasicElement(player, targetElement);
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

    private void assignBasicElement(Player player, ElementType element) {
        if (!player.isOnline()) {
            plugin.getElementManager().cancelRolling(player);
            return;
        }

        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        ElementType oldElement = pd.getCurrentElement();
        SmartEffectCleaner.clearForElementChange(plugin, player);

        plugin.getLogger().info("Assigning " + element + " to " + player.getName() + " (previous: " + oldElement + ")");

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
        plugin.getElementManager().applyUpsides(player);
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        player.sendMessage(ChatColor.GREEN + "Your element has been rerolled to " +
                ChatColor.AQUA + element.name() + ChatColor.GREEN + "!");

        // Mark rolling as complete
        plugin.getElementManager().cancelRolling(player);
    }
}