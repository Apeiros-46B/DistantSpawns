package xyz.apeiros.distantspawns;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSpawnChangeEvent;

public class BedListener implements Listener {
	public BedListener(DistantSpawns plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSetSpawn(PlayerSpawnChangeEvent e) {
		if (e.getCause() != PlayerSpawnChangeEvent.Cause.BED) return;

		Location loc = e.getNewSpawn();
		if (loc == null) return;

		World w = loc.getWorld();
		if (w == null) return;

		double dist = loc.distance(w.getSpawnLocation());
		if (dist >= ConfigManager.getMaxDistance()) {
			e.getPlayer().sendMessage(ConfigManager.getBedTooFarMsg());
			e.setCancelled(true);
		}
	}
}
