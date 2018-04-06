package com.walkertribe.ian.world;

import java.util.SortedMap;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.util.BoolState;

/**
 * A player ship.
 * @author dhleong
 */
public class ArtemisPlayer extends BaseArtemisShip {
    private byte mShipIndex = -1;
    private int mDockingBase = -1;
    private byte mWarp = -1;
    private BoolState mDocked = BoolState.FALSE;

    public ArtemisPlayer(int objId) {
        super(objId);
    }
    
    /**
     * Returns TRUE if the player ship is docked; FALSE otherwise.
     */
    public BoolState isDocked() {
    	return mDocked;
    }
    
    public void setDocked(BoolState docked) {
    	mDocked = docked;
    }
    
    @Override
    public void setImpulse(float impulse) {
    	super.setImpulse(impulse);
    	if (!Float.isNaN(impulse) && impulse > 0) setDocked(BoolState.FALSE);
    }

    @Override
    public ObjectType getType() {
        return ObjectType.PLAYER_SHIP;
    }

    /**
     * Get this ship's player ship index. Note that this value is zero-based, so
     * the vessel that is named Artemis will have a ship index of 0.
     * Unspecified: -1
     * @return int in [0,Artemis.SHIP_COUNT), or -1 if undefined
     */
    public byte getShipIndex() {
        return mShipIndex;
    }

    public void setShipIndex(byte shipIndex) {
    	mShipIndex = shipIndex;
    }

    /**
     * Get the ID of the base at which we're docking. Note that this property is
     * only updated in a packet when the docking process commences; undocking
     * does not update this property. However, if an existing ArtemisPlayer
     * object is docked, is updated by another one, and the update has the ship
     * engaging impulse or warp drive, this property will be set to 0 to
     * indicate that the ship has undocked.
     * Unspecified: -1
     */
    public int getDockingBase() {
        return mDockingBase;
    }

    public void setDockingBase(int baseId) {
        mDockingBase = baseId;
    }

    /**
     * Warp factor, between 0 (not at warp) and Artemis.MAX_WARP.
     * Unspecified: -1
     */
    public byte getWarp() {
    	return mWarp;
    }

    public void setWarp(byte warp) {
    	if (warp < -1 || warp > Artemis.MAX_WARP)
    		throw new IllegalArgumentException("Invalid warp factor: " + warp);
    	
		mWarp = warp;
	}
    
    /**
     * Returns true if this packet contains data.
     */
    @Override
    public boolean hasData() {
    	return  super.hasData() ||
    			mShipIndex != -1 ||
    			mDockingBase != -1 ||
    			mWarp != -1;
    }
    
    public boolean isMoving() {
    	return !Float.isNaN(getImpulse()) && getImpulse() > 0;
    }

    @Override
    public void updateFrom(ArtemisObject obj) {
        super.updateFrom(obj);
        
        if (obj instanceof ArtemisPlayer) {
            ArtemisPlayer plr = (ArtemisPlayer) obj;
            if (mShipIndex == -1) mShipIndex = plr.mShipIndex;
            if (plr.mWarp != -1) mWarp = plr.mWarp;

            if (plr.mDockingBase > 0) {
            	mDockingBase = plr.mDockingBase;
            } else if (plr.isMoving() || plr.mWarp > 0) {
            	mDockingBase = 0;
            }
        }
    }

    @Override
	public void appendObjectProps(SortedMap<String, Object> props) {
    	super.appendObjectProps(props);
    	putProp(props, "Ship index", mShipIndex, -1);
    	putProp(props, "Docking base", mDockingBase, -1);
    	putProp(props, "Warp", mWarp, -1);
    }
}