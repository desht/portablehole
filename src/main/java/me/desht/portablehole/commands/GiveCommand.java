package me.desht.portablehole.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.BookItem;
import me.desht.portablehole.HoleException;
import me.desht.portablehole.PortableHolePlugin;

public class GiveCommand extends AbstractCommand {

	public GiveCommand() {
		super("ph g", 0, 1);
		setPermissionNode("portablehole.commands.give");
		setUsage("/ph give [<playername>]");
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean execute(Plugin plugin, CommandSender sender, String[] args) {
		notFromConsole(sender);
		
		PortableHolePlugin phPlugin = (PortableHolePlugin) plugin;
		
		Player target;
		
		if (args.length == 0) {
			target = (Player) sender;
		} else {
			target = Bukkit.getPlayer(args[0]);
			if (target == null) {
				throw new HoleException("Player '" + args[0] + "' is not online.");
			}
		}
		
		String title = plugin.getConfig().getString("booktitle", "Portable Hole");
		
		BookItem bi = new BookItem(new ItemStack(387,1));
		bi.setTitle(title);
		bi.setAuthor(sender.getName());
		String[] pages = plugin.getConfig().getStringList("text").toArray(new String[0]);
		bi.setPages(pages);
		
		ItemStack writtenbook = bi.getItemStack();
		target.getInventory().addItem(writtenbook);
		target.updateInventory();
		
		String msg = String.format(phPlugin.getMessage("gavebook"), title, target.getName());
		MiscUtil.statusMessage(sender, msg);
		
		return true;
	}

}
