package com.walkertribe.ian.enums;

import java.util.EnumSet;

public enum CommsFilter {
	ALERT, SIDE, STATUS, PLAYER, STATION, ENEMY, FRIEND;
	
	public static EnumSet<CommsFilter> fromBits(int bits) {
		EnumSet<CommsFilter> fromBits = EnumSet.noneOf(CommsFilter.class);
		
		for (CommsFilter type: values()) {
			if ((bits & (1 << type.ordinal())) != 0) {
				fromBits.add(type);
			}
		}
		
		return fromBits;
	}
}