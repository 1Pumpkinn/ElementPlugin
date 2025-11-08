package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
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

        // Validate item
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        var container = meta.getPersistentDataContainer();

        if (!container.has(ItemKeys.advancedReroller(plugin), PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        // Prevent using reroller on pedestal blocks
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.LODESTONE) {
            var blockStorage = plugin.getBlockStorage();
            String blockId = blockStorage.getCustomBlockIdCached(event.getClickedBlock().getLocation());
            if ("pedestal".equalsIgnoreCase(blockId)) return;
        }

        event.setCancelled(true);

        var elementManager = plugin.getElementManager();
        if (elementManager.isCurrentlyRolling(player)) {
            player.sendMessage(ChatColor.RED + "You are already rerolling your element!");
            return;
        }

        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType current = pd.getCurrentElement();
        ElementType newElement = determineNewElement(current);

        // Consume one reroller
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) player.getInventory().removeItem(item);

        performAdvancedRoll(player, newElement);
    }

    private ElementType determineNewElement(ElementType current) {
        return switch (current) {
            case METAL -> ElementType.FROST;
            case FROST -> ElementType.METAL;
            default -> random.nextBoolean() ? ElementType.METAL : ElementType.FROST;
        };
    }

    private void performAdvancedRoll(Player player, ElementType targetElement) {
        plugin.getElementManager().data(player.getUniqueId()); // ensures player data is initialized
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        String[] names = {"METAL", "FROST"};
        ChatColor[] colors = {ChatColor.AQUA};
        int steps = 20;
        long interval = 3L;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= steps) {
                    assignAdvancedElement(player, targetElement);
                    cancel();
                    return;
                }

                int i = tick % 2;
                player.sendTitle(
                        colors[i] + "Rolling...",
                        colors[i] + names[i],
                        0, 10, 0
                );
                tick++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void assignAdvancedElement(Player player, ElementType element) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        clearAllEffects(player, pd);

        int currentUpgradeLevel = pd.getCurrentElementUpgradeLevel();
        pd.setCurrentElementWithoutReset(element);
        pd.setCurrentElementUpgradeLevel(currentUpgradeLevel);
        plugin.getDataStore().save(pd);

        var title = net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("Element Chosen!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(element.name()).color(
                        element == ElementType.METAL
                                ? net.kyori.adventure.text.format.NamedTextColor.GRAY
                                : net.kyori.adventure.text.format.NamedTextColor.AQUA
                ),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(2000),
                        java.time.Duration.ofMillis(500)
                )
        );

        player.showTitle(title);
        plugin.getElementManager().applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

        ChatColor color = element == ElementType.METAL ? ChatColor.GRAY : ChatColor.AQUA;
        player.sendMessage(ChatColor.GREEN + "Your element has been rerolled to " + color + element.name() + ChatColor.GREEN + "!");
    }

    private void clearAllEffects(Player player, PlayerData pd) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (pd.getCurrentElement() == ElementType.LIFE) {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(20.0);
                player.setHealth(Math.min(player.getHealth(), 20.0));
            }
        }
    }
}
