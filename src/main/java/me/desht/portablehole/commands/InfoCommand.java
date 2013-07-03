package me.desht.portablehole.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.CreditManager;
import me.desht.portablehole.PortableHolePlugin;
import me.desht.portablehole.CreditManager.CostCredit;

public class InfoCommand extends AbstractCommand {

	public InfoCommand() {
		super("ph info", 0, 0);
		setPermissionNode("portablehole.commands.info");
		setUsage("/ph info");
	}

	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		PortableHolePlugin phPlugin = (PortableHolePlugin) plugin;

		CreditManager cm = phPlugin.getCreditManager();

		if (sender instanceof Player) {
			String msg = String.format(phPlugin.getMessage("credits"), cm.getCredit((Player) sender));
			MiscUtil.statusMessage(sender, msg);
		}

		MiscUtil.statusMessage(sender, phPlugin.getMessage("cost_list"));
		for (CostCredit cc : cm.getCosts()) {
			MiscUtil.statusMessage(sender, ChatColor.RED + "\u2022 "
					+ ChatColor.YELLOW + cc.getCost().getDescription()
					+ ChatColor.WHITE + " = "
					+ ChatColor.YELLOW + cc.getCredit() + "C");
		}

		return true;
	}

}
