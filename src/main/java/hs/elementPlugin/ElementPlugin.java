package hs.elementPlugin;

import hs.elementPlugin.commands.TrustCommand;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.listeners.CombatListener;
import hs.elementPlugin.listeners.DeathListener;
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
    private ItemManager itemManager;
    private CooldownManager cooldownManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize core services
        this.configManager = new ConfigManager(this);
        this.dataStore = new DataStore(this);
        this.trustManager = new TrustManager(this);
        this.manaManager = new ManaManager(this, dataStore, configManager);
        this.elementManager = new ElementManager(this, dataStore, manaManager, trustManager, configManager);
        this.itemManager = new ItemManager(this, manaManager, configManager);
        this.cooldownManager = new CooldownManager();

        // Register commands
        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("element").setExecutor(new hs.elementPlugin.commands.ElementCommand(elementManager));
        getCommand("mana").setExecutor(new hs.elementPlugin.commands.ManaCommand(manaManager, configManager));

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, elementManager, manaManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(trustManager, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.CraftListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.ItemRuleListener(this, elementManager, manaManager, itemManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.FriendlyMobListener(this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.EarthListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.QuitListener(this, manaManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.GameModeListener(manaManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.RespawnListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.UpgraderListener(this, elementManager), this);

        // Register recipes with delay to ensure server is fully loaded
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Registering recipes...");
            hs.elementPlugin.items.Upgrader1Item.registerRecipe(this);
            hs.elementPlugin.items.Upgrader2Item.registerRecipe(this);
            hs.elementPlugin.items.RerollerItem.registerRecipe(this);
            getLogger().info("Recipes registered successfully");
        }, 20L); // 1 second delay

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
    public ItemManager getItemManager() { return itemManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }
}