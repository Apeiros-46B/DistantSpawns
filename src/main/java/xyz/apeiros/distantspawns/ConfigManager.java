package xyz.apeiros.distantspawns;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import net.md_5.bungee.api.ChatColor;

public class ConfigManager {
	private final File file;
	private final FileConfiguration config;
	private static ConfigManager instance;

	public ConfigManager(DistantSpawns plugin) {
		this.file = new File(plugin.getDataFolder(), "config.yml");
		if (!this.file.exists()) {
			this.file.getParentFile().mkdirs();
			plugin.saveResource("config.yml", false);
		}

		this.config = new YamlConfiguration();
		try {
			this.config.load(this.file);
		} catch (IOException | InvalidConfigurationException e) {
			logError(e);
		}

		instance = this;
	}

	public static boolean reload() {
		try {
			instance.config.load(instance.file);
			return true;
		} catch (IOException | InvalidConfigurationException e) {
			logError(e);
			return false;
		}
	}

	private static void logError(Throwable e) {
		Bukkit.getLogger().log(
			Level.SEVERE,
			"Error occurred while reading DistantSpawns configuration!",
			e
		);
	}

	private String getColored(String key, String def) {
		return ChatColor.translateAlternateColorCodes('&',
			this.config.getString(key, def)
		);
	}

	public static double getMaxDistance() {
		return instance.config.getDouble("bed.max-distance", 2500.0);
	}

	private static String getMaxDistanceFormatted(boolean nether) {
		double scale = nether ? 1.0/8.0 : 1.0;
		return Long.toString(Math.round(getMaxDistance() * scale));
	}

	public static String getBedTooFarMsg() {
		return instance.getColored(
			"bed.too-far-msg",
			"&cYour spawn was not set because you are more than %distance% blocks away from world spawn. Use a respawn anchor."
		)
			.replace("%distance%", getMaxDistanceFormatted(false));
	}

	public static String getAnchorUnit(int capacity) {
		if (capacity == 1) {
			return instance.config.getString("anchor.singular-charge", "charge");
		} else {
			return instance.config.getString("anchor.plural-charges", "charges");
		}
	}

	public static String getAnchorFullMsg(int capacity) {
		return instance.getColored(
			"anchor.full-msg",
			"&cThis anchor can only hold %capacity% %unit%."
		)
			.replace("%capacity%", Integer.toString(capacity))
			.replace("%unit%", getAnchorUnit(capacity));
	}

	public static String getAnchorDepletedMsg() {
		return instance.getColored(
			"anchor.depleted-msg",
			"&cYour respawn anchor can no longer hold any charges and has been destroyed."
		);
	}

	public static String getAnchorLore(int capacity) {
		return instance.getColored(
			"anchor.lore",
			"&7Capacity: &6%capacity%"
		)
			.replace("%capacity%", Integer.toString(capacity))
			.replace("%unit%", getAnchorUnit(capacity));
	}

	public static String getReloadMsg() {
		return instance.getColored(
			"cmd-msgs.reloaded",
			"&7[&5DistantSpawns&7] &fReloaded configuration."
		);
	}

	public static String getReloadFailMsg() {
		return instance.getColored(
			"cmd-msgs.reloaded-fail",
			"&7[&5DistantSpawns&7] &cFailed to reload configuration."
		);
	}
}
