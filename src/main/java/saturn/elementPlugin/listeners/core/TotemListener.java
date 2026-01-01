package saturn.elementPlugin.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalDashAbility;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public class TotemListener implements Listener {

    private final ElementPlugin plugin;

    public TotemListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Stop bleeding
        player.removeMetadata(DeathSlashAbility.META_BLEEDING, plugin);

        // Stop metal dash
        MetalDashAbility dash =
                (MetalDashAbility) plugin.getAbilityManager().getAbility("metal_dash");

        if (dash != null) {
            dash.cancelDash(player);
        }
    }
}
