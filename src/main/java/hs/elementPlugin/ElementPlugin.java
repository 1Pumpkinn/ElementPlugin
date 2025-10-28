package hs.elementPlugin;

import hs.elementPlugin.elements.abilities.AbilityManager;
import hs.elementPlugin.elements.abilities.impl.water.WaterGeyserAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathWitherSkullAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSummonUndeadAbility;
import hs.elementPlugin.commands.TrustCommand;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.elements.ElementRegistry;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.earth.listeners.EarthOreDropListener;
import hs.elementPlugin.elements.upsides.impl.EarthUpsides;
import hs.elementPlugin.listeners.player.*;
import hs.elementPlugin.listeners.items.listeners.*;
import hs.elementPlugin.managers.*;
import hs.elementSmpUtility.blocks.CustomBlockManager;
import hs.elementSmpUtility.commands.CustomBlockCommand;
import hs.elementSmpUtility.commands.PedestalCommand;
import hs.elementSmpUtility.listeners.ChunkListener;
import hs.elementSmpUtility.listeners.PedestalInteractionListener;
import hs.elementSmpUtility.listeners.block.BlockBreakListener;
import hs.elementSmpUtility.listeners.block.BlockPlacementListener;
import hs.elementSmpUtility.recipes.PedestalRecipe;
import hs.elementSmpUtility.storage.BlockDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage;
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
    private AbilityManager abilityManager;
    private ElementRegistry elementRegistry;

    // Pedestal/Custom Block System
    private CustomBlockManager blockManager;
    private BlockDataStorage blockStorage;
    private PedestalDataStorage pedestalStorage;
    private PedestalOwnerStorage ownerStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize core systems
        this.configManager = new ConfigManager(this);
        this.dataStore = new DataStore(this);
        this.trustManager = new TrustManager(this);
        this.manaManager = new ManaManager(this, dataStore, configManager);
        this.abilityManager = new AbilityManager(this);
        this.elementManager = new ElementManager(this, dataStore, manaManager, trustManager, configManager);
        this.itemManager = new ItemManager(this, manaManager, configManager);
        this.cooldownManager = new CooldownManager();

        // Initialize pedestal/custom block system
        this.blockManager = new CustomBlockManager(this);
        this.blockStorage = new BlockDataStorage(this, blockManager);
        this.pedestalStorage = new PedestalDataStorage(this);
        this.ownerStorage = new PedestalOwnerStorage(this);

        // Register abilities
        this.abilityManager.registerAbility(ElementType.WATER, 1, new WaterGeyserAbility(this));
        this.abilityManager.registerAbility(ElementType.DEATH, 1, new DeathSummonUndeadAbility(this));
        this.abilityManager.registerAbility(ElementType.DEATH, 2, new DeathWitherSkullAbility(this));

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        // ========== Register Recipes ==========
        registerRecipes();

        // Start mana system
        this.manaManager.start();
    }

    @Override
    public void onDisable() {
        // Save data and stop background tasks
        if (dataStore != null) {
            dataStore.flushAll();
        }

        if (manaManager != null) {
            manaManager.stop();
        }
    }

    /**
     * Register all commands
     */
    private void registerCommands() {
        // Element system commands
        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("element").setExecutor(new hs.elementPlugin.commands.ElementCommand(this));
        getCommand("mana").setExecutor(new hs.elementPlugin.commands.ManaCommand(manaManager, configManager));
        getCommand("util").setExecutor(new hs.elementPlugin.commands.UtilCommand(this));

        // Pedestal/Custom block commands
        getCommand("customblock").setExecutor(new CustomBlockCommand(blockManager));
        getCommand("pedestal").setExecutor(new PedestalCommand(pedestalStorage, ownerStorage));

        getLogger().info("Commands registered successfully");
    }

    /**
     * Register all listeners
     */
    private void registerListeners() {
        // ========== Register Core Listeners ==========
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, elementManager, manaManager), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(trustManager, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.items.ElementItemDeathListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.items.ElementItemCraftListener(this, elementManager), this);

        // Item listeners
        Bukkit.getPluginManager().registerEvents(new ElementItemUseListener(this, elementManager, itemManager), this);
        Bukkit.getPluginManager().registerEvents(new ElementInventoryProtectionListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new ElementItemDropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ElementItemPickupListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new ElementCombatProjectileListener(itemManager), this);

        // ========== Air Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.air.listeners.FallDamageListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirAbilityListener(elementManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirCombatListener(elementManager), this);

        // ========== Water Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterDrowningImmunityListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterAbilityListener(elementManager, cooldownManager), this);

        // ========== Fire Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireImmunityListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireAbilityListener(elementManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireFriendlyMobListener(this, trustManager), this);

        // ========== Earth Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthCharmListener(elementManager, this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthAbilityListener(elementManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthFriendlyMobListener(this, trustManager), this);
        Bukkit.getPluginManager().registerEvents(new EarthOreDropListener(elementManager), this);

        // ========== Life Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeRegenListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeAbilityListener(elementManager, cooldownManager), this);

        // ========== Death Element ==========
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathRawFoodListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathJoinListener(elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathAbilityListener(elementManager, cooldownManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathFriendlyMobListener(this, trustManager), this);

        // Craft listeners
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.death.DeathElementCraftListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.elements.impl.life.LifeElementCraftListener(this, elementManager), this);

        // Misc listeners
        Bukkit.getPluginManager().registerEvents(new QuitListener(this, manaManager), this);
        Bukkit.getPluginManager().registerEvents(new GameModeListener(manaManager, configManager), this);
        Bukkit.getPluginManager().registerEvents(new PassiveEffectReapplyListener(this, elementManager), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.GUIListener(this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.items.RerollerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new hs.elementPlugin.listeners.items.UpgraderListener(this, elementManager), this);

        // ========== Pedestal/Custom Block Listeners ==========
        Bukkit.getPluginManager().registerEvents(new BlockPlacementListener(blockManager, blockStorage, ownerStorage), this);
        Bukkit.getPluginManager().registerEvents(new BlockBreakListener(blockManager, blockStorage, pedestalStorage, ownerStorage), this);
        Bukkit.getPluginManager().registerEvents(new PedestalInteractionListener(blockManager, blockStorage, pedestalStorage, ownerStorage), this);
        Bukkit.getPluginManager().registerEvents(new ChunkListener(blockStorage, pedestalStorage, ownerStorage), this);

        getLogger().info("Listeners registered successfully");
    }

    /**
     * Register all recipes
     */
    private void registerRecipes() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Registering recipes...");


            // Utility recipes
            hs.elementPlugin.recipes.util.UtilRecipes.registerRecipes(this);


            // Pedestal recipe
            PedestalRecipe pedestalRecipe = new PedestalRecipe(this, blockManager);
            pedestalRecipe.register();

            getLogger().info("Recipes registered successfully");
        }, 20L); // 1-second delay
    }

    // ====== Getters ======
    public DataStore getDataStore() { return dataStore; }
    public ConfigManager getConfigManager() { return configManager; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public ItemManager getItemManager() { return itemManager; }
    public CooldownManager getCooldownManager() { return cooldownManager; }

    // Pedestal/Custom Block System Getters
    public CustomBlockManager getBlockManager() { return blockManager; }
    public BlockDataStorage getBlockStorage() { return blockStorage; }
    public PedestalDataStorage getPedestalStorage() { return pedestalStorage; }
    public PedestalOwnerStorage getOwnerStorage() { return ownerStorage; }
}