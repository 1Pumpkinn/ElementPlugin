package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final ElementPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ElementPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        plugin.getLogger().info("Configuration reloaded successfully");
    }

    public int getMaxMana() {
        return config.getInt("mana.max", 100);
    }

    public int getManaRegenPerSecond() {
        return config.getInt("mana.regen_per_second", 1);
    }

    public boolean isCreativeInfiniteMana() {
        return config.getBoolean("mana.creative_infinite", true);
    }

    public int getAbility1Cost(ElementType type) {
        return getAbilityValue(type, "ability1", "cost", 50);
    }

    public int getAbility2Cost(ElementType type) {
        return getAbilityValue(type, "ability2", "cost", 75);
    }

    public int getAbility1Cooldown(ElementType type) {
        return getAbilityValue(type, "ability1", "cooldown", 0);
    }

    public int getAbility2Cooldown(ElementType type) {
        return getAbilityValue(type, "ability2", "cooldown", 0);
    }

    private int getAbilityValue(ElementType type, String ability, String key, int defaultValue) {
        String path = "abilities." + type.name().toLowerCase() + "." + ability + "." + key;
        return config.getInt(path, defaultValue);
    }

    public int getLifeMaxHealth() {
        return config.getInt("passives.life.max_health", 30);
    }

    public int getLifeCropGrowthRadius() {
        return config.getInt("passives.life.crop_growth_radius", 5);
    }

    public int getLifeCropGrowthInterval() {
        return config.getInt("passives.life.crop_growth_interval", 40);
    }

    public int getDeathHungerRadius() {
        return config.getInt("passives.death.hunger_radius", 5);
    }

    public int getDeathHungerInterval() {
        return config.getInt("passives.death.hunger_interval", 20);
    }

    public int getFrostSpeedOnLeatherBoots() {
        return config.getInt("passives.frost.speed_on_leather_boots", 1);
    }

    public int getFrostSpeedOnIce() {
        return config.getInt("passives.frost.speed_on_ice", 2);
    }

    public boolean isTrustPreventsDamage() {
        return config.getBoolean("combat.trust_prevents_damage", true);
    }

    public double getAirSlowFallingChance() {
        return config.getDouble("combat.air_slow_falling_chance", 0.05);
    }

    public int getAirSlowFallingDuration() {
        return config.getInt("combat.air_slow_falling_duration", 100);
    }

    public int getFireAspectDuration() {
        return config.getInt("combat.fire_aspect_duration", 80);
    }

    public int getHellishFlamesDuration() {
        return config.getInt("combat.hellish_flames_duration", 200);
    }

    public int getDeathClockDuration() {
        return config.getInt("combat.death_clock_duration", 200);
    }

    public int getDeathSlashBleedDuration() {
        return config.getInt("combat.death_slash_bleed_duration", 100);
    }

    public int getDeathSlashDamageInterval() {
        return config.getInt("combat.death_slash_damage_interval", 20);
    }

    public int getMetalChainStunDuration() {
        return config.getInt("combat.metal_chain_stun_duration", 60);
    }

    public int getMetalDashStunDuration() {
        return config.getInt("combat.metal_dash_stun_duration", 100);
    }

    public int getFrostNovaFreezeDuration() {
        return config.getInt("combat.frost_nova_freeze_duration", 100);
    }

    public int getWaterPrisonDuration() {
        return config.getInt("combat.water_prison_duration", 200);
    }

    public boolean isUpgradersDropOnDeath() {
        return config.getBoolean("items.upgraders_drop_on_death", true);
    }

    public boolean isElementItemsOnePerPlayer() {
        return config.getBoolean("items.element_items_one_per_player", true);
    }

    public boolean isForceElementSelection() {
        return config.getBoolean("gui.force_element_selection", true);
    }

    public boolean isReopenOnCloseWithoutSelection() {
        return config.getBoolean("gui.reopen_on_close_without_selection", true);
    }

    public boolean isLogAbilityUsage() {
        return config.getBoolean("debug.log_ability_usage", false);
    }

    public boolean isLogElementAssignment() {
        return config.getBoolean("debug.log_element_assignment", true);
    }

    public boolean isLogManaChanges() {
        return config.getBoolean("debug.log_mana_changes", false);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }
}