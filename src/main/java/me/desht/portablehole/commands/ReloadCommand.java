package me.desht.portablehole.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.PortableHolePlugin;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class ReloadCommand extends AbstractCommand {

	public ReloadCommand() {
		super("ph r", 0, 0);
		setPermissionNode("portablehole.commands.reload");
		setUsage("/ph reload");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		plugin.reloadConfig();

		((PortableHolePlugin)plugin).processConfig();

		MiscUtil.statusMessage(sender, "Plugin config has been reloaded");
		
		return true;
	}

}
