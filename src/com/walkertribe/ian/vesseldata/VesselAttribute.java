package com.walkertribe.ian.vesseldata;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A list of attributes for Vessels. Corresponds to the &lt;vessel
 * broadType=""&gt; attribute in vesselData.xml.
 * @author rjwut
 */
public final class VesselAttribute {
	private static final Set<VesselAttribute> allAttributes = new HashSet<VesselAttribute>();
	private final String name;
	
	// vessel class
	public static final String PLAYER        = "player";
	public static final String BASE          = "base";
	public static final String SINGLESEAT    = "singleseat";
	public static final String SMALL         = "small";
	public static final String MEDIUM        = "medium";
	public static final String LARGE         = "large";
	
	// singleseat class
	public static final String FIGHTER       = "fighter";
	public static final String SHUTTLE       = "shuttle";
	
	// civilian type
	public static final String WARSHIP       = "warship";
	public static final String SCIENCE       = "science";
	public static final String CARGO         = "cargo";
	public static final String LUXURY        = "luxury";
	public static final String TRANSPORT     = "transport";
	
	// behavior
	public static final String CARRIER       = "carrier";
	public static final String ASTEROIDEATER = "asteroideater";
	public static final String ANOMALYEATER  = "anomalyeater";
	public static final String SENTIENT      = "sentient";
	
	// Ben's Mod
	public static final String DESTROYER     = "destroyer";
	public static final String BEAM          = "beam";
	public static final String CORVETTE      = "corvette";
	public static final String TURRETED      = "turreted";
	public static final String SNIPER        = "sniper";
	public static final String IMPULSE       = "impulse";
	public static final String MISSILE       = "missile";
	public static final String CRUISER       = "cruiser";
	public static final String BATTLECRUISER = "battlecruiser";
	public static final String BROADSIDE     = "broadside";
	public static final String DREADNOUGHT   = "dreadnought";
	public static final String CUSTOM        = "custom";
	
	private VesselAttribute(String n) {
		name = n;
	}
	
	public static VesselAttribute get(String n) {
		for (VesselAttribute attr : allAttributes) {
			if (n.equalsIgnoreCase(attr.name)) return attr;
		}
		
		VesselAttribute attr = new VesselAttribute(n.toUpperCase(Locale.getDefault()));
		allAttributes.add(attr);
		return attr;
	}

	/**
	 * Returns a Set containing the VesselAttributes that correspond to the
	 * space-delimited list of attribute names in the given String.
	 */
	public static Set<VesselAttribute> build(String broadType) {
		String[] tokens = broadType.split(" ");
		Set<VesselAttribute> attrs = new HashSet<VesselAttribute>();

		for (String token : tokens) {
			attrs.add(get(token));
		}

		return attrs;
	}
}