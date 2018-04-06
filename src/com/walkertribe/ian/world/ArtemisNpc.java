package com.walkertribe.ian.world;

import java.util.SortedMap;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.util.BoolState;

/**
 * An NPC ship; they may have special abilities, and can be scanned.
 * @author dhleong
 */
public class ArtemisNpc extends BaseArtemisShip {
    private BoolState mEnemy = BoolState.UNKNOWN;

    public ArtemisNpc(int objId) {
        super(objId);
    }

    @Override
    public ObjectType getType() {
        return ObjectType.NPC_SHIP;
    }

    /**
     * Returns BoolState.TRUE if this ship is an enemy, BoolState.FALSE if it's
     * friendly. Note that this only works in Solo mode.
     * Unspecified: BoolState.UNKNOWN
     */
    public BoolState isEnemy() {
    	return mEnemy;
    }

    public void setEnemy(BoolState enemy) {
    	mEnemy = enemy;
    }

    @Override
    public void updateFrom(ArtemisObject npc) {
        super.updateFrom(npc);
        
        // it SHOULD be an ArtemisNpc
        if (npc instanceof ArtemisNpc) {
            ArtemisNpc cast = (ArtemisNpc) npc;
            BoolState enemy = cast.isEnemy();
            if (BoolState.isKnown(enemy)) mEnemy = enemy;
        }
    }

    @Override
	public void appendObjectProps(SortedMap<String, Object> props) {
    	super.appendObjectProps(props);
    	putProp(props, "Is enemy", mEnemy);
    }
}