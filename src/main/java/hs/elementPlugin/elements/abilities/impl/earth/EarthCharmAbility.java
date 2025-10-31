package hs.elementPlugin.elements.abilities.impl.earth;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.impl.earth.EarthElement;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class EarthCharmAbility extends BaseAbility {

    private final hs.elementPlugin.ElementPlugin plugin;
    
    public EarthCharmAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("earth_charm", 75, 30, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        long until = System.currentTimeMillis() + 30_000L;
        player.setMetadata(EarthElement.META_CHARM_NEXT_UNTIL, new FixedMetadataValue(plugin, until));
        player.sendMessage(ChatColor.GOLD + "Punch a mob to charm it for 30s - it will follow you!");
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.YELLOW + "Mob Charm";
    }

    @Override
    public String getDescription() {
        return "Punch a mob to make it follow you for 30 seconds.";
    }
}