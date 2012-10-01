package me.desht.portablehole;

/*
    This file is part of PortableHole

    PortableHole is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    PortableHole is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with PortableHole.  If not, see <http://www.gnu.org/licenses/>.
 */

import me.desht.dhutils.DHUtilsException;
import me.desht.dhutils.MiscUtil;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;

public class PortableholeEventListener implements Listener {

	private PortableHolePlugin plugin;

	public PortableholeEventListener(PortableHolePlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (event.getAction() == Action.LEFT_CLICK_BLOCK && holdingHoleBook(player)) {
			try {
				Hole.create(plugin, event);
				event.setCancelled(true);
			} catch (DHUtilsException e) {
				MiscUtil.errorMessage(player, e.getMessage());
			}
		}
	}

	/**
	 * Stop players getting ported if nether or ender portal blocks are used for the tunnel material.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (plugin.getHoleManager().getHole(event.getFrom()) != null) {
			event.setCancelled(true);
		}
	}

	/**
	 * Stops physics events - including preventing of attachable items popping off the tunnel.
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockPhysics(BlockPhysicsEvent event) {
		Block b = event.getBlock();

		if (plugin.getHoleManager().getHole(b.getLocation()) != null) {
			event.setCancelled(true);
		} else if (b.getState().getData() instanceof Attachable) {
			// stop attachables (signs, levers...) popping off
			Attachable a = (Attachable) b.getState().getData();
			Block attachedBlock = b.getRelative(a.getAttachedFace());
			if (plugin.getHoleManager().getHole(attachedBlock.getLocation()) != null) {
				event.setCancelled(true);
			}
		} else if (b.getType() == Material.SAND || b.getType() == Material.GRAVEL) {
			// stop sand/gravel falling into a tunnel
			if (plugin.getHoleManager().getHole(b.getRelative(BlockFace.DOWN).getLocation()) != null) {
				event.setCancelled(true);
			}
		}
	}

	/**
	 * Prevents liquids flowing in or out of the tunnel
	 * 
	 * @param event
	 */
	@EventHandler(ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (plugin.getHoleManager().getHole(event.getBlock().getLocation()) != null) {
			event.setCancelled(true);
		} else if (plugin.getHoleManager().getHole(event.getToBlock().getLocation()) != null) {
			event.setCancelled(true);
		}
	}

	private boolean holdingHoleBook(Player player) {
		ItemStack i = player.getItemInHand();
	
		if (i.getType() == Material.WRITTEN_BOOK) {
			BookItem book = new BookItem(i);
			return book.getTitle() != null && book.getTitle().equals(plugin.getConfig().getString("booktitle", "Portable Hole"));
			// TODO: check if player is the author, with permission bypass
		} else {
			return false;
		}
	}
}