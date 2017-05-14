package com.walkertribe.ian.world;

import java.util.SortedMap;

import com.walkertribe.ian.Context;
import com.walkertribe.ian.enums.CreatureType;
import com.walkertribe.ian.enums.ObjectType;

/**
 * Various spacefaring creatures (and wrecks)
 */
public class ArtemisCreature extends BaseArtemisOrientable {
	private CreatureType mCreatureType;

    public ArtemisCreature(int objId) {
        super(objId);
    }

	@Override
	public ObjectType getType() {
		return ObjectType.CREATURE;
	}

    @Override
    public void updateFrom(ArtemisObject obj, Context ctx) {
        super.updateFrom(obj, ctx);
        
        if (obj instanceof ArtemisCreature) {
            ArtemisCreature cast = (ArtemisCreature) obj;

            CreatureType creatureType = cast.getCreatureType();

            if (creatureType != null) {
                setCreatureType(creatureType);
            }
        }
    }

    public CreatureType getCreatureType() {
    	return mCreatureType;
    }

    public void setCreatureType(CreatureType creatureType) {
    	mCreatureType = creatureType;
    }

    @Override
	public void appendObjectProps(SortedMap<String, Object> props, boolean includeUnspecified) {
    	super.appendObjectProps(props, includeUnspecified);
    	putProp(props, "Creature type", mCreatureType, includeUnspecified);
    }
}