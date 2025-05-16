package xyz.apeiros.distantspawns;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {
	public Commands(DistantSpawns plugin) {
		plugin.getCommand("distantspawns").setExecutor(this);
	}

	@Override
	public boolean onCommand(
		CommandSender sender,
		Command command,
		String label,
		String[] args
	) {
		if (args.length == 0) {
			return false;
		}
		if (!args[0].equals("reload")) return false;

		if (ConfigManager.reload()) {
			sender.sendMessage(ConfigManager.getReloadMsg());
		} else {
			sender.sendMessage(ConfigManager.getReloadFailMsg());
		}
		return true;
    }
}
