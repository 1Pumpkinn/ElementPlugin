package hs.elementPlugin;

import hs.elementPlugin.commands.*;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.elements.ElementRegistry;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.AbilityManager;
import hs.elementPlugin.elements.abilities.impl.air.*;
import hs.elementPlugin.elements.abilities.impl.frost.*;
import hs.elementPlugin.elements.abilities.impl.water.*;
import hs.elementPlugin.elements.abilities.impl.death.*;
import hs.elementPlugin.elements.abilities.impl.fire.*;
import hs.elementPlugin.elements.abilities.impl.earth.*;
import hs.elementPlugin.elements.abilities.impl.life.*;
import hs.elementPlugin.elements.abilities.impl.metal.*;
import hs.elementPlugin.elements.impl.earth.listeners.EarthOreDropListener;
import hs.elementPlugin.elements.impl.metal.listeners.MetalChainStunListener;
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
    private AbilityManager abilityManager;
    private ElementRegistry elementRegistry;

    private CustomBlockManager blockManager;
    private BlockDataStorage blockStorage;
    private PedestalDataStorage pedestalStorage;
    private PedestalOwnerStorage ownerStorage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeManagers();
        registerAbilities();
        registerCommands();
        registerListeners();
        registerRecipes();
        manaManager.start();
    }

    @Override
    public void onDisable() {
        if (dataStore != null) dataStore.flushAll();
        if (manaManager != null) manaManager.stop();
    }

    private void initializeManagers() {
        this.configManager = new ConfigManager(this);
        this.dataStore = new DataStore(this);
        this.trustManager = new TrustManager(this);
        this.manaManager = new ManaManager(this, dataStore, configManager);
        this.abilityManager = new AbilityManager(this);
        this.elementManager = new ElementManager(this, dataStore, manaManager, trustManager, configManager);
        this.itemManager = new ItemManager(this, manaManager, configManager);

        this.blockManager = new CustomBlockManager(this);
        this.blockStorage = new BlockDataStorage(this, blockManager);
        this.pedestalStorage = new PedestalDataStorage(this);
        this.ownerStorage = new PedestalOwnerStorage(this);
    }

    private void registerAbilities() {
        abilityManager.registerAbility(ElementType.AIR, 1, new AirBlastAbility(this));
        abilityManager.registerAbility(ElementType.AIR, 2, new AirDashAbility(this));

        abilityManager.registerAbility(ElementType.WATER, 1, new WaterGeyserAbility(this));
        abilityManager.registerAbility(ElementType.WATER, 2, new WaterBeamAbility(this));

        abilityManager.registerAbility(ElementType.FIRE, 1, new FireballAbility(this));
        abilityManager.registerAbility(ElementType.FIRE, 2, new MeteorShowerAbility(this));

        abilityManager.registerAbility(ElementType.EARTH, 1, new EarthTunnelAbility(this));
        abilityManager.registerAbility(ElementType.EARTH, 2, new EarthCharmAbility(this));

        abilityManager.registerAbility(ElementType.LIFE, 1, new LifeRegenAbility(this));
        abilityManager.registerAbility(ElementType.LIFE, 2, new LifeHealingBeamAbility(this));

        abilityManager.registerAbility(ElementType.DEATH, 1, new DeathSummonUndeadAbility(this));
        abilityManager.registerAbility(ElementType.DEATH, 2, new DeathWitherSkullAbility(this));

        abilityManager.registerAbility(ElementType.METAL, 1, new hs.elementPlugin.elements.abilities.impl.metal.MetalChainAbility(this));
        abilityManager.registerAbility(ElementType.METAL, 2, new hs.elementPlugin.elements.abilities.impl.metal.MetalDashAbility(this));

        abilityManager.registerAbility(ElementType.FROST, 1, new FrostCircleAbility(this));
        abilityManager.registerAbility(ElementType.FROST, 2, new FrostPunchAbility(this));

        getLogger().info("Registered all element abilities");
    }

    private void registerCommands() {
        ElementInfoCommand elementInfoCmd = new ElementInfoCommand(this);
        getCommand("elements").setExecutor(elementInfoCmd);
        getCommand("elements").setTabCompleter(elementInfoCmd);

        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("element").setExecutor(new ElementCommand(this));
        getCommand("mana").setExecutor(new ManaCommand(manaManager, configManager));
        getCommand("util").setExecutor(new UtilCommand(this));
        getCommand("damagetest").setExecutor(new hs.elementPlugin.util.DamageTester());

        getCommand("customblock").setExecutor(new CustomBlockCommand(blockManager));
        getCommand("pedestal").setExecutor(new PedestalCommand(pedestalStorage, ownerStorage));

        getLogger().info("Commands registered successfully");
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();

        pm.registerEvents(new JoinListener(this, elementManager, manaManager), this);
        pm.registerEvents(new CombatListener(trustManager, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.ElementItemDeathListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.ElementItemCraftListener(this, elementManager), this);

        pm.registerEvents(new ElementItemUseListener(this, elementManager, itemManager), this);
        pm.registerEvents(new ElementInventoryProtectionListener(this, elementManager), this);
        pm.registerEvents(new ElementItemDropListener(this), this);
        pm.registerEvents(new ElementItemPickupListener(this, elementManager), this);
        pm.registerEvents(new ElementCombatProjectileListener(itemManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.FallDamageListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirCombatListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterDrowningImmunityListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterJoinListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireImmunityListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireCombatListener(elementManager, trustManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireballProtectionListener(), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthCharmListener(elementManager, this), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthFriendlyMobListener(this, trustManager), this);
        pm.registerEvents(new EarthOreDropListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeRegenListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeJoinListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathRawFoodListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathFriendlyMobListener(this, trustManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.death.DeathElementCraftListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.life.LifeElementCraftListener(this, elementManager), this);

        pm.registerEvents(new QuitListener(this, manaManager), this);
        pm.registerEvents(new GameModeListener(manaManager, configManager), this);
        pm.registerEvents(new PassiveEffectReapplyListener(this, elementManager), this);
        pm.registerEvents(new PassiveEffectMonitor(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.GUIListener(this), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.RerollerListener(this), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.AdvancedRerollerListener(this), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.UpgraderListener(this, elementManager), this);

        pm.registerEvents(new BlockPlacementListener(blockManager, blockStorage, ownerStorage), this);
        pm.registerEvents(new BlockBreakListener(blockManager, blockStorage, pedestalStorage, ownerStorage), this);
        pm.registerEvents(new PedestalInteractionListener(blockManager, blockStorage, pedestalStorage, ownerStorage), this);
        pm.registerEvents(new ChunkListener(blockStorage, pedestalStorage, ownerStorage), this);
        pm.registerEvents(new hs.elementSmpUtility.listeners.PedestalProtectionListener(blockStorage), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.metal.listeners.MetalJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.metal.listeners.MetalArrowImmunityListener(elementManager), this);
        pm.registerEvents(new MetalChainStunListener(), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostPassiveListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostFrozenPunchListener(this, elementManager), this);

        getLogger().info("Listeners registered successfully");
    }

    private void registerRecipes() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            getLogger().info("Registering recipes...");
            hs.elementPlugin.recipes.util.UtilRecipes.registerRecipes(this);
            new PedestalRecipe(this, blockManager).register();
            hs.elementSmpUtility.recipes.ShulkerBoxRecipe.register(this);
            hs.elementSmpUtility.recipes.ElytraRecipe.register(this);
            getLogger().info("Recipes registered successfully");
        }, 20L);
    }

    public DataStore getDataStore() { return dataStore; }
    public ConfigManager getConfigManager() { return configManager; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public ItemManager getItemManager() { return itemManager; }
    public CustomBlockManager getBlockManager() { return blockManager; }
    public BlockDataStorage getBlockStorage() { return blockStorage; }
    public PedestalDataStorage getPedestalStorage() { return pedestalStorage; }
    public PedestalOwnerStorage getOwnerStorage() { return ownerStorage; }
}