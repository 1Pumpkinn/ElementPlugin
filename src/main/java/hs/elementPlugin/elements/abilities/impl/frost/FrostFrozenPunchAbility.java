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

public class FrostFrozenPunchAbility extends BaseAbility {
    private final ElementPlugin plugin;

    // Metadata key for tracking frozen punch ready state
    public static final String META_FROZEN_PUNCH_READY = "frost_frozen_punch_ready";

    public FrostFrozenPunchAbility(ElementPlugin plugin) {
        super("frost_frozen_punch", 75, 20, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Mark player as ready to freeze next target
        long until = System.currentTimeMillis() + 30_000L; // 30 seconds to use
        player.setMetadata(META_FROZEN_PUNCH_READY, new FixedMetadataValue(plugin, until));

        player.sendMessage(ChatColor.AQUA + "Frozen Punch ready! Hit an enemy to freeze them for 5 seconds!");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

        // Spawn particles around player to indicate ready state
        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 20, 0.5, 0.5, 0.5, 0.1, null, true);

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Frozen Punch";
    }

    @Override
    public String getDescription() {
        return "Your next melee hit freezes the target in place for 5 seconds.";
    }
}