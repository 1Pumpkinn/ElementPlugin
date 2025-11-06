package hs.elementPlugin.elements.abilities.impl.frost;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class FrostPunchAbility extends BaseAbility {
    private final ElementPlugin plugin;
    public static final String META_FROZEN_PUNCH_READY = "frost_frozen_punch_ready";

    public FrostPunchAbility(ElementPlugin plugin) {
        super("frost_frozen_punch", 75, 10, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Set metadata indicating the next punch will freeze
        long until = System.currentTimeMillis() + 10_000L; // 10 seconds to use it
        player.setMetadata(META_FROZEN_PUNCH_READY, new FixedMetadataValue(plugin, until));

        // Visual and audio feedback
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 30, 0.3, 0.3, 0.3, 0.1, null, true);
        player.getWorld().spawnParticle(Particle.CLOUD, loc, 15, 0.3, 0.3, 0.3, 0.05, null, true);

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Frozen Punch";
    }

    @Override
    public String getDescription() {
        return "Your next punch freezes an enemy in place for 5 seconds, preventing all movement.";
    }
}