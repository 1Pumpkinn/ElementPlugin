package hs.elementPlugin;

import hs.elementPlugin.commands.TrustCommand;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.listeners.CombatListener;
import hs.elementPlugin.listeners.JoinListener;
import hs.elementPlugin.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElementPlugin extends JavaPlugin {

    private DataStore dataStore;
    private ConfigManager configManager;
    private ElementManager elementManager;
    private ManaManager manaManager;
    private TrustManager trustManager;
    private CooldownManager cooldownManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize core services
        this.configManager = new ConfigManager(this);
        this.dataStore = new DataStore(this);
        this.trustManager = new TrustManager(this);
        this.cooldownManager = new CooldownManager();
        this.manaManager = new ManaManager(this, dataStore, configManager);
        this.elementManager = new ElementManager(this, dataStore, manaManager, trustManager, cooldownManager, configManager);
        this.itemManager = new ItemManager(this, manaManager, configManager);

        // Register commands
        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("element").setExecutor(new hs.elementPlugin.commands.ElementCommand(elementManager));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, elementManager, manaManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(trustManager, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.CraftListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.ItemRuleListener(this, elementManager, manaManager, itemManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.FriendlyMobListener(this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.EarthListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.QuitListener(this, manaManager), this);

        // Register recipes
        hs.elementPlugin.items.Upgrader1Item.registerRecipe(this);
        hs.elementPlugin.items.Upgrader2Item.registerRecipe(this);

        // Start repeating tasks
        this.manaManager.start();
    }

    @Override
    public void onDisable() {
        // Save data
        if (dataStore != null) {
            dataStore.flushAll();
        }

        if (manaManager != null) {
            manaManager.stop();
        }
    }

    public DataStore getDataStore() { return dataStore; }
    public ConfigManager getConfigManager() { return configManager; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
}