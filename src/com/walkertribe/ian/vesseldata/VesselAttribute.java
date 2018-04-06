package com.walkertribe.ian.vesseldata;

import java.util.Set;

import com.walkertribe.ian.util.Util;

/**
 * A list of attributes for Vessels. Corresponds to the &lt;vessel
 * broadType=""&gt; attribute in vesselData.xml.
 * @author rjwut
 */
public final class VesselAttribute {
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
	
	/**
	 * No instantiation allowed.
	 */
	private VesselAttribute() { }

	/**
	 * Returns a Set containing the VesselAttributes that correspond to the
	 * space-delimited list of attribute names in the given String.
	 */
	public static Set<String> build(CharSequence broadType) {
		return Util.splitSpaceDelimited(broadType);
	}
}