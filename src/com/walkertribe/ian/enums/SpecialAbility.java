package com.walkertribe.ian.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * Special abilities that enemy ships may have.
 * @author rjwut
 */
public enum SpecialAbility {
	STEALTH,      // invisible on long range or tactical views
	LOW_VIS,      // invisible on helm or weapons views until within 3k
	CLOAK,        // invisible on all views
	HET,          // high energy turn
	WARP,         // warp drive
	TELEPORT,     // jump drive
	TRACTOR,      // tractor beam
	DRONES,       // Torgoth drone launcher
	ANTI_MINE,    // can shoot mines
	ANTI_TORP,    // can shoot torpedoes
	SHIELD_DRAIN; // can drain your shields

	/**
	 * Returns a set containing the SpecialAbility values that correspond to the
	 * given bit field value.
	 */
	public static Set<SpecialAbility> fromValue(int value) {
		Set<SpecialAbility> set = new HashSet<SpecialAbility>();

		for (SpecialAbility ability : values()) {
			if ((value & ability.bit) != 0) {
				set.add(ability);
			}
		}

		return set;
	}

	private int bit;

	SpecialAbility() {
		bit = 0x01 << ordinal();
	}

	/**
	 * Given a bit field value, returns true if the bit corresponding to this
	 * SpecialAbility is turned on; false otherwise.
	 */
	public boolean on(int value) {
		return (value & bit) != 0;
	}
}