package hs.elementPlugin;

import hs.elementPlugin.commands.*;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.elements.ElementRegistry;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.AbilityManager;
import hs.elementPlugin.elements.abilities.impl.air.AirBlastAbility;
import hs.elementPlugin.elements.abilities.impl.air.AirDashAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import hs.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import hs.elementPlugin.elements.abilities.impl.earth.EarthTunnelAbility;
import hs.elementPlugin.elements.abilities.impl.fire.HellishFlamesAbility;
import hs.elementPlugin.elements.abilities.impl.fire.PhoenixFormAbility;
import hs.elementPlugin.elements.abilities.impl.frost.FrostNovaAbility;
import hs.elementPlugin.elements.abilities.impl.frost.IceShardVolleyAbility;
import hs.elementPlugin.elements.abilities.impl.life.LifeHealingBeamAbility;
import hs.elementPlugin.elements.abilities.impl.life.LifeRegenAbility;
import hs.elementPlugin.elements.abilities.impl.water.WaterPrisonAbility;
import hs.elementPlugin.elements.abilities.impl.water.WaterWhirlpoolAbility;
import hs.elementPlugin.elements.impl.earth.listeners.EarthOreDropListener;
import hs.elementPlugin.elements.impl.metal.listeners.MetalChainStunListener;
import hs.elementPlugin.listeners.items.DeathListener;
import hs.elementPlugin.listeners.items.listeners.ElementCombatProjectileListener;
import hs.elementPlugin.listeners.items.listeners.ElementItemDropListener;
import hs.elementPlugin.listeners.items.listeners.ElementItemPickupListener;
import hs.elementPlugin.listeners.items.listeners.ElementItemUseListener;
import hs.elementPlugin.listeners.player.*;
import hs.elementPlugin.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class ElementPlugin extends JavaPlugin {

    private DataStore dataStore;
    private ElementManager elementManager;
    private ManaManager manaManager;
    private TrustManager trustManager;
    private ItemManager itemManager;
    private AbilityManager abilityManager;
    private ElementRegistry elementRegistry;

    // Constants
    private static final int MAX_MANA = 100;
    private static final int MANA_REGEN_PER_SECOND = 1;
    private static final int ABILITY_1_COST = 50; // 50
    private static final int ABILITY_2_COST = 75;  // 75
    private static final boolean UPGRADERS_DROP_ON_DEATH = true;

    @Override
    public void onEnable() {
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
        this.dataStore = new DataStore(this);
        this.trustManager = new TrustManager(this);
        this.manaManager = new ManaManager(this, dataStore);
        this.abilityManager = new AbilityManager(this);
        this.elementManager = new ElementManager(this, dataStore, manaManager, trustManager);
        this.itemManager = new ItemManager(this, manaManager);
    }

    private void registerAbilities() {
        abilityManager.registerAbility(ElementType.AIR, 1, new AirBlastAbility(this));
        abilityManager.registerAbility(ElementType.AIR, 2, new AirDashAbility(this));

        abilityManager.registerAbility(ElementType.WATER, 1, new WaterPrisonAbility(this));
        abilityManager.registerAbility(ElementType.WATER, 2, new WaterWhirlpoolAbility(this));

        abilityManager.registerAbility(ElementType.FIRE, 1, new HellishFlamesAbility(this));
        abilityManager.registerAbility(ElementType.FIRE, 2, new PhoenixFormAbility(this));

        abilityManager.registerAbility(ElementType.EARTH, 1, new EarthTunnelAbility(this));
        abilityManager.registerAbility(ElementType.EARTH, 2, new EarthquakeAbility(this));

        abilityManager.registerAbility(ElementType.LIFE, 1, new LifeRegenAbility(this));
        abilityManager.registerAbility(ElementType.LIFE, 2, new LifeHealingBeamAbility(this));

        abilityManager.registerAbility(ElementType.DEATH, 1, new DeathClockAbility(this));
        abilityManager.registerAbility(ElementType.DEATH, 2, new DeathSlashAbility(this));

        abilityManager.registerAbility(ElementType.METAL, 1, new hs.elementPlugin.elements.abilities.impl.metal.MetalChainAbility(this));
        abilityManager.registerAbility(ElementType.METAL, 2, new hs.elementPlugin.elements.abilities.impl.metal.MetalDashAbility(this));

        abilityManager.registerAbility(ElementType.FROST, 1, new IceShardVolleyAbility(this));
        abilityManager.registerAbility(ElementType.FROST, 2, new FrostNovaAbility(this));

        getLogger().info("Registered all element abilities");
    }

    private void registerCommands() {
        ElementInfoCommand elementInfoCmd = new ElementInfoCommand(this);
        getCommand("elements").setExecutor(elementInfoCmd);
        getCommand("elements").setTabCompleter(elementInfoCmd);

        getCommand("trust").setExecutor(new TrustCommand(this, trustManager));
        getCommand("element").setExecutor(new ElementCommand(this));
        getCommand("mana").setExecutor(new ManaCommand(manaManager));
        getCommand("util").setExecutor(new UtilCommand(this));
        getCommand("damagetest").setExecutor(new hs.elementPlugin.util.DamageTester());

        getLogger().info("Commands registered successfully");
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();

        pm.registerEvents(new JoinListener(this, elementManager, manaManager), this);
        pm.registerEvents(new CombatListener(trustManager, elementManager), this);
        pm.registerEvents(new DeathListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathCombatListener(this, elementManager), this);

        pm.registerEvents(new ElementItemUseListener(this, elementManager, itemManager), this);
        pm.registerEvents(new ElementItemDropListener(this), this);
        pm.registerEvents(new ElementItemPickupListener(this, elementManager), this);
        pm.registerEvents(new ElementCombatProjectileListener(itemManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.FallDamageListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.air.listeners.AirCombatListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterDrowningImmunityListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.water.listeners.WaterPrisonMovementListener(), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireImmunityListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireCombatListener(elementManager, trustManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.fire.listeners.FireballProtectionListener(), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.earth.listeners.EarthquakeMovementListener(this), this);
        pm.registerEvents(new EarthOreDropListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeRegenListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.life.listeners.LifeJoinListener(elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathRawFoodListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.death.listeners.DeathFriendlyMobListener(this, trustManager), this);

        pm.registerEvents(new QuitListener(this, manaManager), this);
        pm.registerEvents(new GameModeListener(manaManager), this);
        pm.registerEvents(new PassiveEffectReapplyListener(this, elementManager), this);
        pm.registerEvents(new PassiveEffectMonitor(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.RerollerListener(this), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.AdvancedRerollerListener(this), this);
        pm.registerEvents(new hs.elementPlugin.listeners.items.UpgraderListener(this, elementManager), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.metal.listeners.MetalJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.metal.listeners.MetalArrowImmunityListener(elementManager), this);
        pm.registerEvents(new MetalChainStunListener(), this);

        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostJoinListener(elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostPassiveListener(this, elementManager), this);
        pm.registerEvents(new hs.elementPlugin.elements.impl.frost.listeners.FrostNovaMovementListener(this), this);
        getLogger().info("Listeners registered successfully");
    }

    private void registerRecipes() {
        Bukkit.getScheduler().runTaskLater(this, () -> {
            hs.elementPlugin.recipes.util.UtilRecipes.registerRecipes(this);
            getLogger().info("Recipes registered successfully");
        }, 20L);
    }

    // Getters
    public DataStore getDataStore() { return dataStore; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public TrustManager getTrustManager() { return trustManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }

    // Constants
    public int getMaxMana() { return MAX_MANA; }
    public int getManaRegenPerSecond() { return MANA_REGEN_PER_SECOND; }
    public int getAbility1Cost() { return ABILITY_1_COST; }
    public int getAbility2Cost() { return ABILITY_2_COST; }
    public boolean isUpgradersDropOnDeath() { return UPGRADERS_DROP_ON_DEATH; }
}