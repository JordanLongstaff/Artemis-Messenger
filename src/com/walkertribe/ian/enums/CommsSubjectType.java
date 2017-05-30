package com.walkertribe.ian.enums;

import java.util.EnumSet;

public enum CommsSubjectType {
	ALERT, SIDE, STATUS, PLAYER, STATION, ENEMY, FRIEND;
	
	public static EnumSet<CommsSubjectType> fromBits(int bits) {
		EnumSet<CommsSubjectType> fromBits = EnumSet.noneOf(CommsSubjectType.class);
		
		for (CommsSubjectType type: values()) {
			if ((bits & (1 << type.ordinal())) != 0) {
				fromBits.add(type);
			}
		}
		
		return fromBits;
	}
}