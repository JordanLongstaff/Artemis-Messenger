package com.walkertribe.ian.world;

import com.walkertribe.ian.enums.ObjectType;

/**
 * Bases
 */
public class ArtemisBase extends BaseArtemisShielded {
	public ArtemisBase(int objId) {
        super(objId);
    }

	@Override
    public ObjectType getType() {
        return ObjectType.BASE;
    }
}