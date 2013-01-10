package me.desht.portablehole.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.HoleException;
import me.desht.portablehole.PortableHolePlugin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class GiveCommand extends AbstractCommand {

	public GiveCommand() {
		super("ph g");
		setPermissionNode("portablehole.commands.give");
		setUsage("/ph give [<playername>] [-author <authorname>]");
		setOptions(new String[] { "author:s" });
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		
		PortableHolePlugin phPlugin = (PortableHolePlugin) plugin;
		
		Player target;
		
		if (args.length == 0) {
			notFromConsole(sender);
			target = (Player) sender;
		} else {
			target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				throw new HoleException("Player '" + args[0] + "' is not online.");
			}
		}
		
		String title = plugin.getConfig().getString("book_title", "Portable Hole");
		String author = hasOption("author") ? getStringOption("author") : sender.getName();
		
		ItemStack writtenBook = ((PortableHolePlugin)plugin).makeBookItem(author);
		target.getInventory().addItem(writtenBook);
		target.updateInventory();
		
		String msg = String.format(phPlugin.getMessage("gave_book"), title, target.getName());
		MiscUtil.statusMessage(sender, msg);
		if (!sender.getName().equals(target.getName())) {
			msg = String.format(phPlugin.getMessage("got_book"), title);
			MiscUtil.alertMessage(target, msg);
		}
		return true;
	}

}
