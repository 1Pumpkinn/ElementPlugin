package saturn.elementPlugin;

import saturn.elementPlugin.commands.*;
import saturn.elementPlugin.data.DataStore;
import saturn.elementPlugin.elements.ElementRegistry;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.AbilityManager;
import saturn.elementPlugin.elements.abilities.impl.air.AirBlastAbility;
import saturn.elementPlugin.elements.abilities.impl.air.AirDashAbility;
import saturn.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthTunnelAbility;
import saturn.elementPlugin.elements.abilities.impl.fire.HellishFlamesAbility;
import saturn.elementPlugin.elements.abilities.impl.fire.PhoenixFormAbility;
import saturn.elementPlugin.elements.abilities.impl.frost.FrostNovaAbility;
import saturn.elementPlugin.elements.abilities.impl.frost.IceShardVolleyAbility;
import saturn.elementPlugin.elements.abilities.impl.life.TransfusionAbility;
import saturn.elementPlugin.elements.abilities.impl.life.LifeRegenAbility;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalChainAbility;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalDashAbility;
import saturn.elementPlugin.elements.abilities.impl.water.WaterPrisonAbility;
import saturn.elementPlugin.elements.abilities.impl.water.WaterWhirlpoolAbility;
import saturn.elementPlugin.elements.impl.earth.listeners.EarthOreDropListener;
import saturn.elementPlugin.elements.impl.metal.listeners.MetalChainStunListener;
import saturn.elementPlugin.listeners.core.*;
import saturn.elementPlugin.listeners.items.DeathListener;
import saturn.elementPlugin.listeners.items.handlers.ElementCombatProjectileListener;
import saturn.elementPlugin.listeners.items.handlers.ElementItemDropListener;
import saturn.elementPlugin.listeners.items.handlers.ElementItemPickupListener;
import saturn.elementPlugin.listeners.items.handlers.ElementItemUseListener;
import saturn.elementPlugin.managers.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import saturn.elementPlugin.managers.TeamManager;
import static saturn.elementPlugin.recipes.util.UtilRecipes.registerRecipes;

public final class ElementPlugin extends JavaPlugin {

    private DataStore dataStore;
    private ElementManager elementManager;
    private ManaManager manaManager;
    private TeamManager teamManager;
    private ItemManager itemManager;
    private AbilityManager abilityManager;
    private ElementRegistry elementRegistry;

    // Constants
    private static final int MAX_MANA = 100;
    private static final int MANA_REGEN_PER_SECOND = 1;
    private static final int ABILITY_1_COST = 50;
    private static final int ABILITY_2_COST = 75;
    private static final boolean UPGRADERS_DROP_ON_DEATH = true;

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

    private void initializeManagers() {
        this.dataStore = new DataStore(this);
        this.teamManager = new TeamManager(this);
        this.manaManager = new ManaManager(this, dataStore);
        this.abilityManager = new AbilityManager(this);
        this.elementManager = new ElementManager(this, dataStore, manaManager, teamManager);
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
        abilityManager.registerAbility(ElementType.LIFE, 2, new TransfusionAbility(this));

        abilityManager.registerAbility(ElementType.DEATH, 1, new DeathSlashAbility(this));
        abilityManager.registerAbility(ElementType.DEATH, 2, new DeathClockAbility(this));

        abilityManager.registerAbility(ElementType.METAL, 1, new MetalDashAbility(this));
        abilityManager.registerAbility(ElementType.METAL, 2, new MetalChainAbility(this));

        abilityManager.registerAbility(ElementType.FROST, 1, new IceShardVolleyAbility(this));
        abilityManager.registerAbility(ElementType.FROST, 2, new FrostNovaAbility(this));

        getLogger().info("Registered all element abilities");
    }

    private void registerCommands() {
        ElementInfoCommand elementInfoCmd = new ElementInfoCommand(this);
        getCommand("elements").setExecutor(elementInfoCmd);
        getCommand("elements").setTabCompleter(elementInfoCmd);

        // UPDATED: Only team command (trust removed)
        getCommand("team").setExecutor(new TeamCommand(this, teamManager));
        getCommand("team").setTabCompleter(new TeamCommand(this, teamManager));

        getCommand("element").setExecutor(new ElementCommand(this));
        getCommand("mana").setExecutor(new ManaCommand(manaManager));
        getCommand("util").setExecutor(new UtilCommand(this));
        getCommand("damagetest").setExecutor(new saturn.elementPlugin.util.DamageTester());
        getCommand("data").setExecutor(new DataCommand(this));

        getLogger().info("Commands registered successfully (Trust system removed)");
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();

        // Core listeners
        pm.registerEvents(new JoinListener(this, elementManager, manaManager, teamManager), this);
        pm.registerEvents(new QuitListener(this, manaManager, teamManager), this);
        pm.registerEvents(new CombatListener(teamManager, elementManager), this);
        pm.registerEvents(new DeathListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.AbilityListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathCombatListener(this, elementManager, teamManager), this);

        // Item listeners
        pm.registerEvents(new ElementItemUseListener(this, elementManager, itemManager), this);
        pm.registerEvents(new ElementItemDropListener(this), this);
        pm.registerEvents(new ElementItemPickupListener(this, elementManager), this);
        pm.registerEvents(new ElementCombatProjectileListener(itemManager), this);

        // Air element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.air.listeners.FallDamageListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.air.listeners.AirJoinListener(elementManager), this);

        // Water element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.water.listeners.WaterDrowningImmunityListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.water.listeners.WaterJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.water.listeners.WaterPrisonMovementListener(), this);

        // Fire element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireImmunityListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.fire.listeners.FireCombatListener(elementManager, teamManager), this);

        // Earth element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.earth.listeners.EarthquakeMovementListener(this), this);
        pm.registerEvents(new EarthOreDropListener(elementManager), this);

        // Life element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.life.listeners.LifeHungerListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.life.listeners.LifeJoinListener(elementManager), this);

        // Death element listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathCombatListener(this, elementManager, teamManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.death.listeners.DeathXPDropListener(elementManager), this);

        // Game management listeners
        pm.registerEvents(new GameModeListener(manaManager), this);
        pm.registerEvents(new PassiveEffectReapplyListener(this, elementManager), this);
        pm.registerEvents(new PassiveEffectMonitor(this, elementManager), this);

        // Reroller listeners
        pm.registerEvents(new saturn.elementPlugin.listeners.items.RerollerListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.items.AdvancedRerollerListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.listeners.items.UpgraderListener(this, elementManager), this);

        // Metal listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.metal.listeners.MetalJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.metal.listeners.MetalKnockbackListener(elementManager), this);
        pm.registerEvents(new MetalChainStunListener(), this);

        // Frost listeners
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostJoinListener(elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostPassiveListener(this, elementManager), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostNovaMovementListener(this), this);
        pm.registerEvents(new saturn.elementPlugin.elements.impl.frost.listeners.FrostCombatListener(elementManager, teamManager), this);

        getLogger().info("Listeners registered successfully (Team-based system only)");
    }

    // Getters
    public DataStore getDataStore() { return dataStore; }
    public ElementManager getElementManager() { return elementManager; }
    public ManaManager getManaManager() { return manaManager; }
    public TeamManager getTrustManager() { return teamManager; }
    public ItemManager getItemManager() { return itemManager; }
    public AbilityManager getAbilityManager() { return abilityManager; }

    // Constants
    public int getMaxMana() { return MAX_MANA; }
    public int getManaRegenPerSecond() { return MANA_REGEN_PER_SECOND; }
    public int getAbility1Cost() { return ABILITY_1_COST; }
    public int getAbility2Cost() { return ABILITY_2_COST; }
    public boolean isUpgradersDropOnDeath() { return UPGRADERS_DROP_ON_DEATH; }
}