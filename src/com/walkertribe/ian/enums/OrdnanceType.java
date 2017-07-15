package com.walkertribe.ian.enums;

/**
 * The types of ordnance that player ships can fire.
 * @author rjwut
 */
public enum OrdnanceType {
	HOMING("Homing", 1, 180000),
	NUKE("Nuke", 4, 600000),
	MINE("Mine", 6, 240000),
	EMP("EMP", 9, 300000),
	PSHOCK("Pshock", 8, 600000);

	public static final int COUNT = values().length;

	private final String label;
	private final int type, time;

	OrdnanceType(String label, int type, int time) {
		this.label = label;
		this.type = type;
		this.time = time;
	}

	@Override
	public String toString() {
		return label;
	}
	
	public int getType() {
		return type;
	}
	
	public int getBuildTime() {
		return time;
	}
}