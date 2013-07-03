package me.desht.portablehole;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.desht.dhutils.Cost;
import me.desht.dhutils.LogUtils;
import me.desht.dhutils.MiscUtil;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class CreditManager {
	private final PortableHolePlugin plugin;
	private final List<CostCredit> costs = new ArrayList<CostCredit>();
	private final Map<String, Integer> credits = new HashMap<String,Integer>();

	public CreditManager(PortableHolePlugin plugin) {
		this.plugin = plugin;

		loadCosts();
	}

	public void loadCosts() {
		List<?> l = plugin.getConfig().getList("costs");

		costs.clear();
		for (Object o : l) {
			@SuppressWarnings("unchecked")
			Map<String, Object> m = (Map<String,Object>) o;
			String cost = (String)m.get("cost");
			int credit = m.containsKey("blocks") ? (Integer)m.get("blocks") : 1;
			LogUtils.fine("load cost: cost=" + cost + " credit=" + credit);
			try {
				Cost c = new Cost(cost);
				costs.add(new CostCredit(c, credit));
			} catch (IllegalArgumentException e) {
				LogUtils.warning("invalid cost/credit definition (ignored): [" + cost + "/" + credit + "] - " + e.getMessage());
			}
		}
	}

	public void loadCredits() {
		File f = new File(plugin.getDataFolder(), "credits.yml");
		if (!f.exists())
			return;

		Configuration cfg = YamlConfiguration.loadConfiguration(f);
		for (String k : cfg.getKeys(false)) {
			credits.put(k, cfg.getInt(k));
		}
	}

	public void saveCredits() {
		File f = new File(plugin.getDataFolder(), "credits.yml");

		YamlConfiguration cfg = new YamlConfiguration();
		for (Entry<String, Integer> entry :	credits.entrySet()) {
			cfg.set(entry.getKey(), entry.getValue());
		}

		try {
			cfg.save(f);
		} catch (IOException e) {
			LogUtils.severe("can't save " + f + ": " + e.getMessage());
		}
	}

	public boolean buyCredit(Player p, int creditWanted) {
		for (CostCredit cc : costs) {
			Cost cost = cc.getCost();
			int credit = cc.getCredit();
			int units = (int) Math.ceil((double)creditWanted / (double)credit);
			Cost actualCost = new Cost(cost.getType(), cost.getId(), cost.getData(), cost.getQuantity() * units);

			LogUtils.fine("check cost: " + cost + " = credit: " + credit + ", units = " + units + ", actual cost = " + actualCost);
			if (actualCost.isAffordable(p)) {
				LogUtils.fine("affordable!");
				actualCost.chargePlayer(p);
				if (plugin.getConfig().getBoolean("verbose_charges")) {
					MiscUtil.statusMessage(p, String.format(plugin.getMessage("charge_made"), actualCost.getDescription()));
				}
				giveCredit(p, units * credit);
				return true;
			}
		}
		return false;
	}

	public void giveCredit(Player p, int credit) {
		String playerName = p.getName();
		credits.put(playerName, getCredit(p) + credit);
		LogUtils.fine("give " + credit + " credit to " + playerName + " - now has " + credits.get(playerName) + " credits");
	}

	public void takeCredit(Player player, int credit) {
		giveCredit(player, -credit);
	}

	public int getCredit(Player p) {
		String playerName = p.getName();
		if (!credits.containsKey(playerName)) {
			credits.put(playerName, 0);
		}
		return credits.get(playerName);
	}

	public List<CostCredit>getCosts() {
		return costs;
	}

	public class CostCredit {
		private final Cost cost;
		private final int credit;

		public CostCredit(Cost c, int credit) {
			this.cost = c;
			this.credit = credit;
		}

		public Cost getCost() {
			return cost;
		}

		public int getCredit() {
			return credit;
		}
	}
}
