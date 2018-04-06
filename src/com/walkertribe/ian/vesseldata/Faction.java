package com.walkertribe.ian.vesseldata;

import java.util.Set;

import com.walkertribe.ian.util.Version;

/**
 * Corresponds to the <hullRace> element in vesselData.xml.
 * @author rjwut
 */
public class Faction {
	private int id;
	private String name;
	private Set<String> attributes;
	
	private static Version version;
	private static final Version TERRAN_VERSION = new Version("2.3.0");

	Faction(int id, String name, String keys) {
		this.id = id;
		this.name = name;
		attributes = FactionAttribute.build(keys);
	}

	/**
	 * Returns the faction's ID.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the faction's name.
	 */
	public String getName() {
		return version.ge(TERRAN_VERSION) ? name : name.replaceAll("Terran", "TSN");
	}

	/**
	 * Returns an array containing the FactionAttributes that correspond to this
	 * Faction.
	 */
	public String[] getAttributes() {
		return attributes.toArray(new String[attributes.size()]);
	}

	/**
	 * Returns true if this Faction has all the given FactionAttributes; false
	 * otherwise.
	 */
	public boolean is(String... attrs) {
		for (String attr: attrs) {
			if (!attributes.contains(attr)) return false;
		}
		return true;
	}
	
	/**
	 * Sets the Artemis version.
	 */
	public static void setVersion(Version v) {
		version = v;
	}
}