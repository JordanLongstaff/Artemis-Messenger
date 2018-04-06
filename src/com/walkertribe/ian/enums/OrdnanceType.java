package com.walkertribe.ian.enums;

import java.util.EnumSet;

import com.walkertribe.ian.util.Version;

/**
 * The types of ordnance that player ships can fire.
 * @author rjwut
 */
public enum OrdnanceType {
	HOMING("Torpedo", 1, 180000),
	NUKE("Nuke", 4, 600000),
	MINE("Mine", 6, 240000),
	EMP("EMP", 9, 300000),
	PSHOCK("Pshock", 8, 600000),
	BEACON("Beacon", 5, 60000),
	PROBE("Probe", 2, 60000),
	TAG("Tag", 7, 60000);

	private static final EnumSet<OrdnanceType> ALL_ORDNANCES = EnumSet.allOf(OrdnanceType.class);
	private static final Version PSHOCK_VERSION = new Version("2.1.5");
	private static final Version BEACON_VERSION = new Version("2.6.3");

	private final String label;
	private final int type, time;
	
	private static boolean fullName = false;

	OrdnanceType(String label, int type, int time) {
		this.label = label;
		this.type = type;
		this.time = time;
	}

	@Override
	public String toString() {
		return (fullName ? "Type " + type + " " : "") + getLabel();
	}
	
	public String getLabel() {
		if (fullName && this == HOMING) return "Homing";
		else return label;
	}
	
	public int getType() {
		return type;
	}
	
	public int getBuildTime() {
		return time;
	}
	
	public static EnumSet<OrdnanceType> ordnances() {
		return EnumSet.copyOf(ALL_ORDNANCES);
	}
	
	public static void reconcile(Version version) {
		fullName = version.lt(BEACON_VERSION);
		if (fullName) {
			ALL_ORDNANCES.remove(BEACON);
			ALL_ORDNANCES.remove(TAG);
			ALL_ORDNANCES.remove(PROBE);
		} else {
			ALL_ORDNANCES.add(BEACON);
			ALL_ORDNANCES.add(TAG);
			ALL_ORDNANCES.add(PROBE);
		}
		
		if (version.lt(PSHOCK_VERSION)) {
			ALL_ORDNANCES.remove(PSHOCK);
		} else {
			ALL_ORDNANCES.add(PSHOCK);
		}
	}
	
	public OrdnanceType next() {
		return values()[(ordinal() + 1) % ALL_ORDNANCES.size()];
	}
}