package xyz.apeiros.distantspawns;

import org.bukkit.block.Block;
import org.bukkit.plugin.java.JavaPlugin;

import com.jeff_media.customblockdata.CustomBlockData;

public final class DistantSpawns extends JavaPlugin {
	private static DistantSpawns instance;

	@Override
	public void onEnable() {
		instance = this;
		new ConfigManager(this);
		new AnchorListener(this);
		new BedListener(this);
		new Commands(this);
		CustomBlockData.registerListener(this);
	}

	@Override
	public void onDisable() {
		instance = null;
	}

	public static DistantSpawns getInstance() {
		return instance;
	}

	public static CustomBlockData getDataFor(Block block) {
		return new CustomBlockData(block, instance);
	}
}
