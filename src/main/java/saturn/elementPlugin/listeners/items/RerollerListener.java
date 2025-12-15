package saturn.elementPlugin.listeners.items;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.items.ItemKeys;
import saturn.elementPlugin.util.SmartEffectCleaner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Handles basic reroller usage - rerolls to basic elements (AIR, WATER, FIRE, EARTH)
 * Animation shows cycling through element names before final selection
 */
public class RerollerListener implements Listener {
    private static final ElementType[] BASIC_ELEMENTS = {
            ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH
    };
    private static final String[] ELEMENT_NAMES = {"AIR", "WATER", "FIRE", "EARTH"};
    private static final int ANIMATION_STEPS = 20;
    private static final long ANIMATION_INTERVAL_TICKS = 3L;

    private final ElementPlugin plugin;
    private final Random random = new Random();

    public RerollerListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRerollerUse(PlayerInteractEvent event) {
        if (!isValidRerollerUse(event)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        if (plugin.getElementManager().isCurrentlyRolling(player)) {
            return; // Silent ignore if already rolling
        }

        startReroll(player, event.getItem());
    }


    // VALIDATION
    private boolean isValidRerollerUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return false;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return false;

        return item.getItemMeta().getPersistentDataContainer()
                .has(ItemKeys.reroller(plugin), PersistentDataType.BYTE);
    }

    // REROLL FLOW
    private void startReroll(Player player, ItemStack item) {
        var elementManager = plugin.getElementManager();
        elementManager.setCurrentlyRolling(player, true);

        // Clear old effects immediately
        SmartEffectCleaner.clearForElementChange(plugin, player);

        // Determine new element (guaranteed different)
        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType newElement = selectDifferentElement(pd.getCurrentElement());

        // Consume item
        consumeItem(player, item);

        // Start animation
        playRollingAnimation(player, newElement);
    }

    private ElementType selectDifferentElement(ElementType current) {
        List<ElementType> available = new ArrayList<>();

        for (ElementType element : BASIC_ELEMENTS) {
            if (element != current) {
                available.add(element);
            }
        }

        return available.isEmpty()
                ? BASIC_ELEMENTS[random.nextInt(BASIC_ELEMENTS.length)]
                : available.get(random.nextInt(available.size()));
    }

    private void consumeItem(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            player.getInventory().removeItem(item);
        }
    }

    // ANIMATION
    private void playRollingAnimation(Player player, ElementType targetElement) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    plugin.getElementManager().cancelRolling(player);
                    cancel();
                    return;
                }

                if (tick >= ANIMATION_STEPS) {
                    finishReroll(player, targetElement);
                    cancel();
                    return;
                }

                showAnimationFrame(player, tick);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, ANIMATION_INTERVAL_TICKS);
    }

    private void showAnimationFrame(Player player, int tick) {
        String elementName = ELEMENT_NAMES[tick % ELEMENT_NAMES.length];
        player.sendTitle(
                ChatColor.GOLD + "Rolling...",
                ChatColor.AQUA + elementName,
                0, 10, 0
        );
    }

    private void finishReroll(Player player, ElementType newElement) {
        if (!player.isOnline()) {
            plugin.getElementManager().cancelRolling(player);
            return;
        }

        assignNewElement(player, newElement);
        showCompletionEffects(player, newElement);
        plugin.getElementManager().cancelRolling(player);
    }

    // ELEMENT ASSIGNMENT
    private void assignNewElement(Player player, ElementType element) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());

        // Log the change
        ElementType oldElement = pd.getCurrentElement();
        plugin.getLogger().info("Rerolling " + player.getName() + ": " +
                oldElement + " → " + element);

        // Clear effects again (safety)
        SmartEffectCleaner.clearForElementChange(plugin, player);

        // Preserve upgrade level when rerolling
        int upgradeLevel = pd.getCurrentElementUpgradeLevel();
        pd.setCurrentElementWithoutReset(element);
        pd.setCurrentElementUpgradeLevel(upgradeLevel);

        plugin.getDataStore().save(pd);
        plugin.getElementManager().applyUpsides(player);
    }

    private void showCompletionEffects(Player player, ElementType element) {
        // Title
        Title title = Title.title(
                Component.text("Element Chosen!").color(NamedTextColor.GOLD),
                Component.text(element.name()).color(NamedTextColor.AQUA),
                Title.Times.times(
                        Duration.ofMillis(500),
                        Duration.ofMillis(2000),
                        Duration.ofMillis(500)
                )
        );
        player.showTitle(title);

        // Sound & message
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "Your element has been rerolled to " +
                ChatColor.AQUA + element.name() + ChatColor.GREEN + "!");
    }
}