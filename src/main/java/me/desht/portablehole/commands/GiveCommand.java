package me.desht.portablehole.commands;

import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.commands.AbstractCommand;
import me.desht.portablehole.HoleException;
import me.desht.portablehole.PortableHolePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.Plugin;

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
		
		String title = plugin.getConfig().getString("book_title", "Portable Hole");
		
//		BookMeta bm = (BookMeta)Bukkit.getItemFactory().getItemMeta(Material.WRITTEN_BOOK);
//		bm.setTitle(title);
//		bm.setAuthor(sender.getName());
//		bm.setPages(plugin.getConfig().getStringList("default_book_text"));
//		
//		ItemStack writtenBook = new ItemStack(Material.WRITTEN_BOOK, 1);
//		writtenBook.setItemMeta(bm);
		
		ItemStack writtenBook = ((PortableHolePlugin)plugin).makeBookItem(sender.getName());
		target.getInventory().addItem(writtenBook);
		target.updateInventory();
		
		String msg = String.format(phPlugin.getMessage("gave_book"), title, target.getName());
		MiscUtil.statusMessage(sender, msg);
		
		return true;
	}

}
