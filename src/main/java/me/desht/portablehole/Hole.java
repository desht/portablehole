package me.desht.portablehole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.dhutils.LogUtils;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class Hole {

//	private static final Set<Material> defaultBlockers = new HashSet<Material>();
//	private static final Set<Material> whiteList = new HashSet<Material>();
	private static final Set<Material> blackList = new HashSet<Material>();
//	private static final Set<Material> terminators = new HashSet<Material>();

	private static final Effect DEFAULT_EFFECT = Effect.ENDER_SIGNAL;

	// how far can you tunnel?
	private static final int DEFAULT_MAX_DISTANCE = 31;

	private final Cuboid tunnelExtent;
	private final List<BlockState> blockBackup = new ArrayList<BlockState>();
	private final BukkitTask closeTaskId;
	private final BukkitTask particleTaskId;
	private final int holeId;
	private final BlockFace direction;
	private final boolean locked;
	private final String owner;
	private final long creationTime;
	private final long lifeTime;	// in ticks
	private final PortableHolePlugin plugin;
	private final Block creationBlock;

	private Hole(PortableHolePlugin plugin, PlayerInteractEvent event) {

		this.plugin = plugin;

		Player player = event.getPlayer();

		creationBlock = event.getClickedBlock();
		BlockFace f = event.getBlockFace();

		owner = event.getPlayer().getName();

		locked = false;

		direction = f.getOppositeFace();

		tunnelExtent = getTunnelExtent(player, creationBlock, direction);

		int tunnelLength = getLength();

		creationTime = System.currentTimeMillis();

		if (!PermissionUtils.isAllowedTo(event.getPlayer(), "portablehole.cost.exempt")) {
			// apply tunnelling costs to the player
			CreditManager cm = plugin.getCreditManager();
			int credit = cm.getCredit(player);
			int creditNeeded = tunnelLength - credit;
			LogUtils.fine("player credit: " + credit + ", length = " + tunnelLength + ", credits needed = " + creditNeeded);
			if (creditNeeded > 0) {
				if (!cm.buyCredit(player, creditNeeded)) {
					throw new HoleException(plugin.getMessage("cant_afford"));
				}
			}
			cm.takeCredit(player, tunnelLength);
		}

		// empty out the tunnel, setting it to AIR
		for (Block tunnelBlock : tunnelExtent) {
			tunnelBlock.setTypeIdAndData(0, (byte) 0, false);
		}

		lifeTime = plugin.getConfig().getLong("lifetime.initial") + tunnelLength * plugin.getConfig().getLong("lifetime.per_length");

		// add a delayed task to restore the tunnel's original materials
		closeTaskId = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				close(false);
			}
		}, lifeTime);

		if (!plugin.getConfig().getString("particle_effect").isEmpty()) {
			particleTaskId = Bukkit.getScheduler().runTaskTimer(plugin, new ParticleHandler(), 1L, 50L);
		} else {
			particleTaskId = null;
		}

		// add this hole to the list of current holes
		holeId = plugin.getHoleManager().addHole(this);

		plugin.getFX().playEffect(creationBlock.getLocation(), "hole_open");
	}

	public void close(boolean closeEarly) {
		restoreBlocks();

		plugin.getHoleManager().removeHole(holeId);

		if (closeEarly) {
			closeTaskId.cancel();
		}

		if (particleTaskId != null) {
			particleTaskId.cancel();
		}

		plugin.getFX().playEffect(creationBlock.getLocation(), "hole_close");
	}

	public Cuboid getExtent() {
		return tunnelExtent;
	}

	public int getId() {
		return holeId;
	}

	public String getOwner() {
		return owner;
	}

	public BlockFace getDirection() {
		return direction;
	}

	/**
	 * Get the length of this hole in blocks.
	 * 
	 * @return
	 */
	public int getLength() {
		int tunnelSize = tunnelExtent.volume();
		if (direction.getModY() == 0) { // horizontal tunnel
			tunnelSize /= 2;
		}
		return tunnelSize;
	}

	public boolean isLocked() {
		return locked;
	}

	private void restoreBlocks() {
		for (BlockState bs : blockBackup) {
			if (bs.getBlock().getType() == Material.AIR) {
				bs.update(true);
			}
		}
	}

	/**
	 * Work out where a new hole would go, starting from the given block, going in the given direction.
	 *
	 * @param player	Player who is creating the hole
	 * @param b	Starting block (block clicked by player)
	 * @param direction	Direction the hole will ho
	 */
	private Cuboid getTunnelExtent(Player p, Block b, BlockFace direction) {
		boolean isHorizontal = direction.getModY() == 0;
		Block b1 = b;
		int nTunnelled = 0;
		int max_dist = isHorizontal ?plugin.getConfig().getInt("max_tunnel_length.horizontal", DEFAULT_MAX_DISTANCE) : plugin.getConfig().getInt("max_tunnel_length.vertical", DEFAULT_MAX_DISTANCE);

		boolean overrideProtection = PermissionUtils.isAllowedTo(p, "portablehole.override.protection") || p.getGameMode() == GameMode.CREATIVE;
		boolean voidOK = plugin.getConfig().getBoolean("void_tunnelling");

		List<BlockState> states = new ArrayList<BlockState>();
		while (true) {
			if (!checkBlock(overrideProtection ? null : p, b1, states, voidOK, false)) {
				break;
			}
			if (isHorizontal && !checkBlock(overrideProtection ? null : p, b1.getRelative(BlockFace.DOWN), states, voidOK, true)) {
				break;
			}
			b1 = b1.getRelative(direction);
			if (++nTunnelled > max_dist) {
				throw new HoleException(plugin.getMessage("too_deep"));
			}
		}
		if (b.equals(b1)) {
			// we didn't get to tunnel at all
			throw new HoleException(plugin.getMessage("cant_create"));
		}

		b1 = b1.getRelative(direction.getOppositeFace());	// pull back one block

		// b is our start block
		// b1 is the last tunnellable block

		Cuboid extent = new Cuboid(b.getLocation(), b1.getLocation());
		if (isHorizontal) {
			extent = extent.expand(CuboidDirection.Down, 1);
		}

		blockBackup.clear();
		for (BlockState bs : states) {
			blockBackup.add(bs);
		}

		return extent;
	}

	private boolean checkBlock(Player player, Block b, List<BlockState> states, boolean voidOK, boolean lower) {
		if (player != null) {
			BlockBreakEvent breakEvent = new BlockBreakEvent(b, player);
			Bukkit.getPluginManager().callEvent(breakEvent);
			if (breakEvent.isCancelled()) {
				LogUtils.finer("Break block " + b + " cancelled by other plugin");
				return false;
			}
		}

		if (b.getY() == 0 && !voidOK) {
			LogUtils.finer("Block " + b + " opens into the void");
			return false;
		} else if (blackList.contains(b.getType())) {
			LogUtils.finer("Block " + b + " is blacklisted");
			return false;
		} else if (!lower && !isSolid(b.getType())) {
			LogUtils.finer("Block " + b + " is not solid");
			return false;
		}

		BlockState bs = b.getState();
		if (bs instanceof InventoryHolder) {
			LogUtils.finer("Block " + b + " is an inventory holder");
			return false;
		}
		LogUtils.finer("Block " + b + " is tunnellable");
		states.add(bs);
		return true;
	}

	private boolean isSolid(Material mat) {
		if (mat.getId() == 171 || mat == Material.LADDER) {
			// These materials are considered not solid by Bukkit
			// But for tunnelling purpose, they are in fact pretty solid
			return true;
		} else {
			return mat.isSolid();
		}
	}

	public static Hole create(PortableHolePlugin plugin, PlayerInteractEvent event) {
		PermissionUtils.requirePerms(event.getPlayer(), "portablehole.create");

		validateAuthor(event.getPlayer().getItemInHand());

		Hole h = new Hole(plugin, event);

		LogUtils.fine("created hole: extents = " + h.getExtent());

		return h;
	}

	/**
	 * Check that the author of the given book is in the list of players (or groups)
	 * allowed to create holes.
	 * 
	 * @param item
	 */
	private static void validateAuthor(ItemStack item) {
		PortableHolePlugin plugin = PortableHolePlugin.getInstance();
		Set<String> validAuthors = plugin.getValidAuthors();
		Set<String> validGroups = plugin.getValidGroups();

		if (validAuthors.isEmpty() && validGroups.isEmpty()) {
			return;
		}

		BookMeta bm = (BookMeta) item.getItemMeta();

		if (validAuthors.contains(bm.getAuthor())) {
			return;
		}

		if (!validGroups.isEmpty() && plugin.getPermissionHandler() != null) {
			Permission perm = plugin.getPermissionHandler();
			for (String group : validGroups) {
				if (perm.playerInGroup((String)null, bm.getAuthor(), group)) {
					return;
				}
			}
		}

		throw new HoleException(plugin.getMessage("not_valid_author"));
	}

	public static void initMaterials(PortableHolePlugin plugin) {
		blackList.clear();
		for (int id : plugin.getConfig().getIntegerList("tunnellable.blacklist")) {
			blackList.add(Material.getMaterial(id));
		}
	}

	private class ParticleHandler implements Runnable {

		private Effect e;

		public ParticleHandler() {
			String effectName = plugin.getConfig().getString("particle_effect");
			e = Effect.valueOf(effectName.toUpperCase());
			if (e == null) {
				LogUtils.warning("unknown effect " + effectName + ": defaulting to " + DEFAULT_EFFECT);
				e = DEFAULT_EFFECT;
			} else if (e.getType() != Effect.Type.VISUAL) {
				LogUtils.warning("effect " + effectName + "is not visual: defaulting to " + DEFAULT_EFFECT);
				e = DEFAULT_EFFECT;
			}
		}

		@Override
		public void run() {
			// lifetime of tunnel in milliseconds
			double lifeMillis = lifeTime * 50L;
			double aliveFor = System.currentTimeMillis() - creationTime;

			double prob = (lifeMillis - aliveFor) / (lifeMillis * 1.5);

			World w = tunnelExtent.getWorld();
			for (Block tunnelBlock : tunnelExtent) {
				if (Math.random() < prob) {
					Vector vec = new Vector(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
					w.playEffect(tunnelBlock.getLocation().add(vec), e, 0);
				}
			}
		}

	}
}
