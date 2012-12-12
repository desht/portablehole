package me.desht.portablehole;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.desht.dhutils.BookItem;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;
import me.desht.dhutils.PermissionUtils;
import me.desht.dhutils.cuboid.Cuboid;
import me.desht.dhutils.cuboid.Cuboid.CuboidDirection;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Hole {

	private static final Set<Material> defaultBlockers = new HashSet<Material>();
	private static final Set<Material> whiteList = new HashSet<Material>();
	private static final Set<Material> blackList = new HashSet<Material>();
	private static final Set<Material> terminators = new HashSet<Material>();

	private static final Effect DEFAULT_EFFECT = Effect.ENDER_SIGNAL;

	// how far can you tunnel?
	private static final int DEFAULT_MAX_DISTANCE = 31;

	private final Cuboid tunnelExtent;
	private final List<BlockState> blockBackup;
	private final int closeTaskId;
	private final int particleTaskId;
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

		// store the blocks that currently make up the tunnel
		// also check here that we're actually allowed to tunnel by other plugins...
		blockBackup = new ArrayList<BlockState>();
		for (Block tunnelBlock : tunnelExtent) {
			BlockBreakEvent breakEvent = new BlockBreakEvent(tunnelBlock, player);
			Bukkit.getPluginManager().callEvent(breakEvent);
			if (breakEvent.isCancelled()) {
				throw new HoleException(plugin.getMessage("stopped"));
			}
			blockBackup.add(tunnelBlock.getState());
		}

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
		closeTaskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				close(false);
			}
		}, lifeTime);

		if (!plugin.getConfig().getString("particle_effect").isEmpty()) {
			particleTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new ParticleHandler(), 1L, 50L);
		} else {
			particleTaskId = -1;
		}

		// add this hole to the list of current holes
		holeId = plugin.getHoleManager().addHole(this);

		MiscUtil.playNamedSound(creationBlock.getLocation(), plugin.getConfig().getString("sounds.hole_open"), 1.0f, 1.0f);
	}

	public void close(boolean force) {
		restoreBlocks();

		plugin.getHoleManager().removeHole(holeId);

		if (force) Bukkit.getScheduler().cancelTask(closeTaskId);

		if (particleTaskId != -1)
			Bukkit.getScheduler().cancelTask(particleTaskId);

		MiscUtil.playNamedSound(creationBlock.getLocation(), plugin.getConfig().getString("sounds.hole_close"), 1.0f, 1.0f);
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
		if (direction.getModY() == 0)	// horizontal tunnel
			tunnelSize /= 2;
		return tunnelSize;
	}

	public boolean isLocked() {
		return locked;
	}

	private void restoreBlocks() {
		for (BlockState bs : blockBackup) {
			bs.update(true);
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
		int max_dist = isHorizontal ? plugin.getConfig().getInt("max_tunnel_length.horizontal", DEFAULT_MAX_DISTANCE) : plugin.getConfig().getInt("max_tunnel_length.vertical", DEFAULT_MAX_DISTANCE);
		boolean voidTunnelling = plugin.getConfig().getBoolean("void_tunnelling", false);
		
		do {
			if (!isTunnellable(b1) || (isHorizontal && !isTunnellable(b1.getRelative(BlockFace.DOWN)))) {
				throw new HoleException(plugin.getMessage("cant_create"));
			}
			if (b1.getY() <= 1 && !voidTunnelling) {
				// stop the player from opening a tunnel into the void
				throw new HoleException(plugin.getMessage("cant_create"));
			}
			if (isTerminator(b1) && !b1.equals(b)) {
				// we've reached the end of the tunnel
				break;
			}
			if (++nTunnelled > max_dist) {
				throw new HoleException(plugin.getMessage("too_deep"));
			}

			b1 = b1.getRelative(direction);
		} while (true);
		b1 = b1.getRelative(direction.getOppositeFace());	// pull back one

		// b is our start block
		// b1 is the last tunnellable block

		Cuboid extent = new Cuboid(b.getLocation(), b1.getLocation());
		if (isHorizontal) extent = extent.expand(CuboidDirection.Down, 1);

		return extent;
	}

	private boolean isTerminator(Block b) {
		return terminators.contains(b.getType());
	}

	private boolean isTunnellable(Block b) {
		Material mat = b.getType();

		LogUtils.finer("tunnellable " + b + ": blocked=" + !defaultBlockers.contains(mat) +
		               ", terminated=" + terminators.contains(mat) +
		               ", whitelist=" + whiteList.contains(mat) +
		               ", blacklist=" + blackList.contains(mat));

		if (blackList.contains(mat)) return false;
		if (whiteList.contains(mat)) return true;

		if (PortableHolePlugin.getInstance().getHoleManager().getHole(b.getLocation()) != null) 
			return false;	// overlapping holes not allowed

		return !defaultBlockers.contains(mat); // || terminators.contains(mat);
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

		BookItem book = new BookItem(item);

		if (validAuthors.contains(book.getAuthor())) {
			return;
		}

		if (!validGroups.isEmpty() && plugin.getPermissionHandler() != null) {
			Permission perm = plugin.getPermissionHandler();
			for (String group : validGroups) {
				if (perm.playerInGroup((String)null, book.getAuthor(), group)) {
					return;
				}
			}
		}

		throw new HoleException(plugin.getMessage("not_valid_author"));
	}

	public static void initMaterials(PortableHolePlugin plugin) {
		// these materials will always be tunnellable (careful what you add to this!)
		whiteList.clear();
		for (int id : plugin.getConfig().getIntegerList("tunnellable.whitelist")) {
			whiteList.add(Material.getMaterial(id));
		}

		// these materials will terminate the tunnel creation successfully
		// (the matched block will not be included in the tunnel)
		terminators.clear();
		terminators.add(Material.AIR);
		terminators.add(Material.BEDROCK);
		terminators.add(Material.POWERED_RAIL);
		terminators.add(Material.DETECTOR_RAIL);
		terminators.add(Material.TORCH);
		terminators.add(Material.WATER);
		terminators.add(Material.STATIONARY_WATER);
		terminators.add(Material.SAPLING);
		terminators.add(Material.WEB);
		terminators.add(Material.LONG_GRASS);
		terminators.add(Material.DEAD_BUSH);
		terminators.add(Material.YELLOW_FLOWER);
		terminators.add(Material.RED_ROSE);
		terminators.add(Material.BROWN_MUSHROOM);
		terminators.add(Material.RED_MUSHROOM);
		terminators.add(Material.CROPS);
		terminators.add(Material.SNOW);
		terminators.add(Material.SUGAR_CANE);
		terminators.add(Material.PUMPKIN_STEM);
		terminators.add(Material.MELON_STEM);
		terminators.add(Material.VINE);
		terminators.add(Material.WATER_LILY);
		terminators.add(Material.NETHER_WARTS);
		terminators.add(Material.RAILS);
		terminators.add(Material.LEVER);
		terminators.add(Material.STONE_PLATE);
		terminators.add(Material.WOOD_PLATE);
		terminators.add(Material.REDSTONE_TORCH_OFF);
		terminators.add(Material.REDSTONE_TORCH_ON);
		terminators.add(Material.REDSTONE_WIRE);
		terminators.add(Material.DIODE_BLOCK_OFF);
		terminators.add(Material.DIODE_BLOCK_ON);
		terminators.add(Material.TRIPWIRE);
		terminators.add(Material.TRIPWIRE_HOOK);
		terminators.add(Material.SIGN_POST);
		terminators.add(Material.WALL_SIGN);
		terminators.add(Material.STONE_BUTTON);
		terminators.add(Material.FIRE);

		// encountering any of these materials will cause tunnel creation to fail
		blackList.clear();
		for (int id : plugin.getConfig().getIntegerList("tunnellable.blacklist")) {
			blackList.add(Material.getMaterial(id));
		}

		defaultBlockers.clear();
		defaultBlockers.add(Material.LADDER);
		defaultBlockers.add(Material.PORTAL);
		defaultBlockers.add(Material.ENDER_PORTAL);
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
