package me.desht.portablehole.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.PortableHolePlugin;

public class InfoCommand extends AbstractCommand {

	public InfoCommand() {
		super("ph i", 0, 0);
		setPermissionNode("portablehole.commands.info");
		setUsage("/ph info");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		PortableHolePlugin phPlugin = (PortableHolePlugin) plugin;
		
		if (sender instanceof Player) {
			String msg = String.format(phPlugin.getMessage("credits"), phPlugin.getCreditManager().getCredit((Player) sender));
			MiscUtil.statusMessage(sender, msg);
		}
		
		return true;
	}

}
