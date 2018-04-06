package com.walkertribe.ian.vesseldata;

import java.util.Set;

import com.walkertribe.ian.ArtemisContext;

/**
 * Corresponds to the <vessel> element in vesselData.xml. Note that this
 * represents an entire class of ships, not an individual one.
 * @author rjwut
 */
public class Vessel {
	private ArtemisContext ctx;
	private int id;
	private int side;
	private String name;
	private Set<String> attributes;
	float productionCoeff;

	Vessel(ArtemisContext ctx, int uniqueID, int side, String className, String broadType) {
		this.ctx = ctx;
		id = uniqueID;
		this.side = side;
		name = className;
		attributes = VesselAttribute.build(broadType);
	}

	/**
	 * Returns the Vessel's ID.
	 */
	public int getId() {
		return id;
	}

	/**
	 * Returns the Faction to which this Vessel belongs.
	 */
	public Faction getFaction() {
		return ctx.getVesselData().getFaction(side);
	}

	/**
	 * Returns this Vessel's name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns this Vessel's full name, including its faction's name.
	 */
	public String getFullName() {
		return getFaction().getName() + " " + name;
	}

	/**
	 * Returns true if this Vessel has all the given VesselAttributes; false
	 * otherwise.
	 */
	public boolean is(String... attrs) {
		for (String attr: attrs) {
			if (!attributes.contains(attr)) return false;
		}
		return true;
	}

	/**
	 * Returns the base production coefficient. This value affects how quickly
	 * the base produces new ordnance.
	 */
	public float getProductionCoeff() {
		return productionCoeff;
	}
}