package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.managers.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ManaManager {
    private final ElementPlugin plugin;
    private final DataStore store;
    private final ConfigManager configManager;
    private BukkitTask task;

    private final Map<UUID, PlayerData> cache = new HashMap<>();

    public ManaManager(ElementPlugin plugin, DataStore store, ConfigManager configManager) {
        this.plugin = plugin;
        this.store = store;
        this.configManager = configManager;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int maxMana = configManager.getMaxMana();
            int regenRate = configManager.getManaRegenPerSecond();

            for (Player p : Bukkit.getOnlinePlayers()) {
                PlayerData pd = get(p.getUniqueId());

                // Creative mode players have infinite mana
                if (p.getGameMode() == GameMode.CREATIVE) {
                    pd.setMana(maxMana);
                } else {
                    // Normal mana regen for survival/adventure/spectator
                    int before = pd.getMana();
                    if (before < maxMana) {
                        pd.addMana(regenRate);
                        // Ensure we don't exceed max mana
                        if (pd.getMana() > maxMana) {
                            pd.setMana(maxMana);
                        }
                        store.save(pd);
                    }
                }

                // Fire Upside 2: auto-smelt ores when upgrade >=2
                autoSmeltIfFireUpside2(p, pd);

                // Action bar display with mana emoji
                String manaDisplay = p.getGameMode() == GameMode.CREATIVE ? "∞" : String.valueOf(pd.getMana());
                p.sendActionBar(ChatColor.AQUA + "🔮 Mana: " + ChatColor.WHITE + manaDisplay + ChatColor.GRAY + "/" + maxMana);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, store::load);
    }

    public void save(UUID uuid) {
        PlayerData pd = cache.get(uuid);
        if (pd != null) store.save(pd);
    }

    public boolean spend(Player player, int amount) {
        // Creative mode players don't spend mana
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }

        PlayerData pd = get(player.getUniqueId());
        if (pd.getMana() < amount) return false;
        pd.addMana(-amount);
        store.save(pd);
        return true;
    }
    
    /**
     * Check if player has enough mana without spending it
     * @param player The player to check
     * @param amount The amount of mana required
     * @return true if player has enough mana, false otherwise
     */
    public boolean hasMana(Player player, int amount) {
        // Creative mode players always have mana
        if (player.getGameMode() == GameMode.CREATIVE) {
            return true;
        }
        
        PlayerData pd = get(player.getUniqueId());
        return pd.getMana() >= amount;
    }

    private void autoSmeltIfFireUpside2(Player p, PlayerData pd) {
        if (pd.getCurrentElement() != hs.elementPlugin.elements.ElementType.FIRE) return;
        if (pd.getUpgradeLevel(hs.elementPlugin.elements.ElementType.FIRE) < 2) return;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            var it = inv.getItem(i);
            if (it == null) continue;
            var out = mapSmeltOutput(it.getType());
            if (out != null) {
                int amount = it.getAmount();
                inv.setItem(i, new org.bukkit.inventory.ItemStack(out, amount));
            }
        }
    }

    private org.bukkit.Material mapSmeltOutput(org.bukkit.Material in) {
        switch (in) {
            case RAW_IRON, IRON_ORE, DEEPSLATE_IRON_ORE -> { return org.bukkit.Material.IRON_INGOT; }
            case RAW_GOLD, GOLD_ORE, DEEPSLATE_GOLD_ORE -> { return org.bukkit.Material.GOLD_INGOT; }
            case RAW_COPPER, COPPER_ORE, DEEPSLATE_COPPER_ORE -> { return org.bukkit.Material.COPPER_INGOT; }
            case ANCIENT_DEBRIS -> { return org.bukkit.Material.NETHERITE_SCRAP; }
            default -> { return null; }
        }
    }
}