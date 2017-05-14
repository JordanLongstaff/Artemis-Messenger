package com.walkertribe.ian.enums;

/**
 * The types of ordnance that player ships can fire.
 * @author rjwut
 */
public enum OrdnanceType {
	HOMING("Homing", 1),
	NUKE("Nuke", 4),
	MINE("Mine", 6),
	EMP("EMP", 9),
	PSHOCK("Pshock", 8);

	public static final int COUNT = values().length;

	private final String label;
	private final int type;

	OrdnanceType(String label, int type) {
		this.label = label;
		this.type = type;
	}

	@Override
	public String toString() {
		return label;
	}
	
	public int getType() {
		return type;
	}
}