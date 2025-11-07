package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import org.bukkit.Bukkit;
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

                // Action bar display with mana emoji
                String manaDisplay = p.getGameMode() == GameMode.CREATIVE ? "∞" : String.valueOf(pd.getMana());
                p.sendActionBar(
                    net.kyori.adventure.text.Component.text("Ⓜ Mana: ")
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                        .append(net.kyori.adventure.text.Component.text(manaDisplay, net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .append(net.kyori.adventure.text.Component.text("/" + maxMana, net.kyori.adventure.text.format.NamedTextColor.GRAY))
                );
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
}
