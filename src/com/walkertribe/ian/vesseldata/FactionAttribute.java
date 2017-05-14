package com.walkertribe.ian.vesseldata;

import java.util.HashSet;
import java.util.Set;

/**
 * A list of attributes for Factions. Corresponds to the &lt;hullRace
 * keys=""&gt; attribute in vesselData.xml.
 * @author rjwut
 */
public final class FactionAttribute {
	private static final Set<FactionAttribute> allAttributes = new HashSet<FactionAttribute>();
	private final String name;
	
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

	private FactionAttribute(String n) {
		name = n;
	}

	public static FactionAttribute get(String n) {
		for (FactionAttribute attr: allAttributes) {
			if (n.equalsIgnoreCase(attr.name)) return attr;
		}
		
		FactionAttribute attr = new FactionAttribute(n);
		allAttributes.add(attr);
		return attr;
	}

	/**
	 * Returns a Set containing the FactionAttributes that correspond to the
	 * space-delimited list of attribute names in the given String.
	 */
	public static Set<FactionAttribute> build(String keys) {
		String[] tokens = keys.split(" ");
		Set<FactionAttribute> attrs = new HashSet<FactionAttribute>();

		for (String token : tokens) {
			attrs.add(get(token));
		}

		return attrs;
	}
}