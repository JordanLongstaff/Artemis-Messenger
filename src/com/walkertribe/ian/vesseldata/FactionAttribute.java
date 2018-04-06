package com.walkertribe.ian.vesseldata;

import java.util.Set;

import com.walkertribe.ian.util.Util;

/**
 * A list of attributes for Factions. Corresponds to the &lt;hullRace
 * keys=""&gt; attribute in vesselData.xml.
 * @author rjwut
 */
public final class FactionAttribute {
	// stance
	public static final String PLAYER      = "player";
	public static final String FRIENDLY    = "friendly";
	public static final String ENEMY       = "enemy";
	public static final String BIOMECH     = "biomech";
	
	// fleet behavior
	public static final String STANDARD    = "standard";
	public static final String SUPPORT     = "support";
	public static final String LONER       = "loner";
	
	// behavior
	public static final String WHALELOVER  = "whalelover";
	public static final String WHALEHATER  = "whalehater";
	public static final String HASSPECIALS = "hasspecials";
	public static final String JUMPMASTER  = "jumpmaster";

	/**
	 * No instantiation allowed.
	 */
	private FactionAttribute() { }
	
	public static Set<String> build(String keys) {
		return Util.splitSpaceDelimited(keys);
	}
}