package saturn.elementPlugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import saturn.elementPlugin.commands.*;
import saturn.elementPlugin.data.DataStore;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.AbilityManager;
import saturn.elementPlugin.elements.abilities.impl.air.*;
import saturn.elementPlugin.elements.abilities.impl.death.*;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import saturn.elementPlugin.elements.abilities.impl.fire.*;
import saturn.elementPlugin.elements.abilities.impl.frost.*;
import saturn.elementPlugin.elements.abilities.impl.life.*;
import saturn.elementPlugin.elements.abilities.impl.metal.*;
import saturn.elementPlugin.elements.abilities.impl.water.*;
import saturn.elementPlugin.elements.impl.earth.listeners.EarthOreDropListener;
import saturn.elementPlugin.elements.impl.metal.listeners.MetalChainStunListener;
import saturn.elementPlugin.listeners.core.*;
import saturn.elementPlugin.listeners.items.DeathListener;
import saturn.elementPlugin.listeners.items.handlers.*;
import saturn.elementPlugin.managers.*;
import saturn.elementPlugin.regions.DisabledRegionsManager;

import static saturn.elementPlugin.recipes.util.UtilRecipes.registerRecipes;

public final class ElementPlugin extends JavaPlugin {

    private DataStore dataStore;
    private ElementManager elementManager;
    private ManaManager manaManager;
    private ItemManager itemManager;
    private AbilityManager abilityManager;
    private DisabledRegionsManager disabledRegionsManager;

    // ========================
    // Constants
    // ========================

    private static final int MAX_MANA = 100;
    private static final int MANA_REGEN_PER_SECOND = 1;
    private static final int ABILITY_1_COST = 50;
    private static final int ABILITY_2_COST = 75;
    private static final boolean UPGRADERS_DROP_ON_DEATH = true;

    // ========================
    // Plugin Lifecycle
    // ========================

    @Override
    public void onEnable() {
        initializeManagers();
        registerAbilities();
        registerCommands();
        registerListeners();
        registerRecipes(this);
        manaManager.start();
    }

    @Override
    public void onDisable() {
        if (dataStore != null) dataStore.flushAll();
        if (manaManager != null) manaManager.stop();
    }

    // ========================
    // Initialization
    // ========================

    private void initializeManagers() {
        this.dataStore = new DataStore(this);
        this.manaManager = new ManaManager(this, dataStore);
        this.abilityManager = new AbilityManager(this);
        this.elementManager = new ElementManager(this, dataStore, manaManager);
        this.itemManager = new ItemManager(this, manaManager);
        this.disabledRegionsManager = new DisabledRegionsManager(this);
    }

    // ========================
    // Ability Registration
    // ========================

    private void registerAbilities() {
        abilityManager.registerAbility(ElementType.AIR, 1, new AirBlastAbility(this));
        abilityManager.registerAbility(ElementType.AIR, 2, new AirDashAbility(this));

        abilityManager.registerAbility(ElementType.WATER, 1, new WaterPrisonAbility(this));
        abilityManager.registerAbility(ElementType.WATER, 2, new WaterWhirlpoolAbility(this));

        abilityManager.registerAbility(ElementType.FIRE, 1, new HellishFlamesAbility(this));
        abilityManager.registerAbility(ElementType.FIRE, 2, new PhoenixFormAbility(this));

        abilityManager.registerAbility(ElementType.EARTH, 2, new EarthquakeAbility(this));

        abilityManager.registerAbility(ElementType.LIFE, 1, new LifeRegenAbility(this));
        abilityManager.registerAbility(ElementType.LIFE, 2, new TransfusionAbility(this));

        abilityManager.registerAbility(ElementType.DEATH, 1, new DeathSlashAbility(this));
        abilityManager.registerAbility(ElementType.DEATH, 2, new DeathClockAbility(this));

        abilityManager.registerAbility(ElementType.METAL, 1, new MetalDashAbility(this));
        abilityManager.registerAbility(ElementType.METAL, 2, new MetalChainAbility(this));

        abilityManager.registerAbility(ElementType.FROST, 1, new IceShardVolleyAbility(this));
        abilityManager.registerAbility(ElementType.FROST, 2, new FrostNovaAbility(this));

        getLogger().info("Registered all element abilities");
    }

    // ========================
    // Commands
    // ========================

    private void registerCommands() {
        ElementInfoCommand infoCommand = new ElementInfoCommand(this);
        getCommand("elements").setExecutor(infoCommand);
        getCommand("elements").setTabCompleter(infoCommand);

        getCommand("element").setExecutor(new ElementCommand(this));
        getCommand("mana").setExecutor(new ManaCommand(manaManager));
        getCommand("util").setExecutor(new UtilCommand(this));
        getCommand("damagetest").setExecutor(new saturn.elementPlugin.util.DamageTester());
        getCommand("data").setExecutor(new DataCommand(this));

        DisableAbilitiesCommand disableCmd = new DisableAbilitiesCommand(this, disabledRegionsManager);
        getCommand("disableabilities").setExecutor(disableCmd);
        getCommand("disableabilities").setTabCompleter(disableCmd);

        getLogger().info("Commands registered successfully");
    }

    // ========================
    // Listeners
    // ========================

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();

        // Core
        pm.registerEvents(new JoinListener(this, elementManager, manaManager), this);
        pm.registerEvents(new QuitListener(this, manaManager), this);
        pm.registerEvents(new CombatListener(elementManager), this);
        pm.registerEvents(new DeathListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.AbilityListener(this, elementManager), this);

        // Items
        pm.registerEvents(new ElementItemUseListener(this, elementManager, itemManager), this);
        pm.registerEvents(new ElementItemDropListener(this), this);
        pm.registerEvents(new ElementItemPickupListener(this, elementManager), this);
        pm.registerEvents(new ElementCombatProjectileListener(itemManager), this);

        // ========================
        // Utility Items (CRITICAL FIX - These were missing!)
        // ========================
        pm.registerEvents(new saturn.elementPlugin.listeners.items.UpgraderListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.items.RerollerListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.items.AdvancedRerollerListener(this), this);

        // ========================
        // Air
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.air.listeners.AirJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.air.listeners.AirCombatListener(elementManager), this);

        // ========================
        // Water
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.water.listeners.WaterJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.water.listeners.WaterPrisonMovementListener(), this);

        // ========================
        // Fire
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireImmunityListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireCombatListener(elementManager), this);

        // ========================
        // Earth
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.earth.listeners.EarthquakeMovementListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.earth.listeners.EarthGoldenAppleListener(elementManager), this);
        pm.registerEvents(new EarthOreDropListener(elementManager), this);

        // ========================
        // Life
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.life.listeners.LifeHungerListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.life.listeners.LifeJoinListener(elementManager), this);

        // ========================
        // Death
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathInvisibilityListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathXPDropListener(elementManager), this);

        // ========================
        // Metal
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.metal.listeners.MetalJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.metal.listeners.MetalKnockbackListener(elementManager), this);
        pm.registerEvents(new MetalChainStunListener(), this);

        // ========================
        // Frost
        // ========================
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostPassiveListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostNovaMovementListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostCombatListener(elementManager), this);

        getLogger().info("Listeners registered successfully");
    }

    // ========================
    // Getters
    // ========================

    public DataStore getDataStore() { return dataStore; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }
    public DisabledRegionsManager getDisabledRegionsManager() { return disabledRegionsManager; }

    public int getMaxMana() { return MAX_MANA; }
    public int getManaRegenPerSecond() { return MANA_REGEN_PER_SECOND; }
    public int getAbility1Cost() { return ABILITY_1_COST; }
    public int getAbility2Cost() { return ABILITY_2_COST; }
    public boolean isUpgradersDropOnDeath() { return UPGRADERS_DROP_ON_DEATH; }
}