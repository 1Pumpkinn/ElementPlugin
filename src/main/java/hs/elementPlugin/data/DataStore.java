package hs.elementPlugin.data;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataStore {
    private final ElementPlugin plugin;

    private final File playerFile;
    private final FileConfiguration playerCfg;

    public DataStore(ElementPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) dataDir.mkdirs();
        this.playerFile = new File(dataDir, "players.yml");
        if (!playerFile.exists()) {
            try { playerFile.createNewFile(); } catch (IOException ignored) {}
        }
        this.playerCfg = YamlConfiguration.loadConfiguration(playerFile);
    }

    public synchronized PlayerData load(UUID uuid) {
        String key = uuid.toString();
        ConfigurationSection sec = playerCfg.getConfigurationSection(key);
        PlayerData pd = new PlayerData(uuid);
        if (sec != null) {
            String elem = sec.getString("element");
            if (elem != null) pd.setCurrentElement(ElementType.valueOf(elem));
            pd.setMana(sec.getInt("mana", 100));
            ConfigurationSection up = sec.getConfigurationSection("upgrades");
            if (up != null) {
                for (String name : up.getKeys(false)) {
                    try {
                        ElementType t = ElementType.valueOf(name);
                        pd.setUpgradeLevel(t, up.getInt(name, 0));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
            java.util.List<String> items = sec.getStringList("items");
            if (items != null) {
                for (String name : items) {
                    try {
                        ElementType t = ElementType.valueOf(name);
                        pd.addElementItem(t);
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return pd;
    }

    public synchronized void save(PlayerData pd) {
        String key = pd.getUuid().toString();
        ConfigurationSection sec = playerCfg.getConfigurationSection(key);
        if (sec == null) sec = playerCfg.createSection(key);
        sec.set("element", pd.getCurrentElement() == null ? null : pd.getCurrentElement().name());
        sec.set("mana", pd.getMana());
        Map<String, Integer> up = new HashMap<>();
        for (Map.Entry<ElementType, Integer> e : pd.getUpgradesView().entrySet()) {
            up.put(e.getKey().name(), e.getValue());
        }
        sec.createSection("upgrades", up);
        java.util.List<String> items = new java.util.ArrayList<>();
        for (ElementType t : pd.getOwnedItems()) items.add(t.name());
        sec.set("items", items);
        flushQuiet();
    }

    public synchronized void flushAll() {
        flushQuiet();
    }

    private void flushQuiet() {
        try { playerCfg.save(playerFile); } catch (IOException ignored) {}
    }

    // TRUST store
    public synchronized Set<UUID> getTrusted(UUID owner) {
        ConfigurationSection sec = playerCfg.getConfigurationSection(owner + ".trust");
        Set<UUID> set = new HashSet<>();
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                try { set.add(UUID.fromString(k)); } catch (IllegalArgumentException ignored) {}
            }
        }
        return set;
    }

    public synchronized void setTrusted(UUID owner, Set<UUID> trusted) {
        String base = owner + ".trust";
        playerCfg.set(base, null);
        ConfigurationSection sec = playerCfg.createSection(base);
        for (UUID u : trusted) sec.set(u.toString(), true);
        flushQuiet();
    }
}