package me.desht.portablehole;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;

public class HoleManager {

	private final Map<Integer, Hole> allHoles = new HashMap<Integer, Hole>();

	private int nextId;

	public HoleManager() {
		nextId = 0;
	}

	public int addHole(Hole hole) {
		allHoles.put(nextId, hole);
		return nextId++;
	}

	public void removeHole(int id) {
		allHoles.remove(id);
	}

	public Hole getHole(int id) {
		return allHoles.get(id);
	}

	public Hole getHole(Location loc) {
		for (Hole h : allHoles.values()) {
			if (h.getExtent().contains(loc)) {
				return h;
			}
		}
		return null;
	}

	public Collection<Hole> getHoles() {
		return allHoles.values();
	}
}
