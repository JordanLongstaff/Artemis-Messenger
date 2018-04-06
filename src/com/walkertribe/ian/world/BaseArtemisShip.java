package com.walkertribe.ian.world;

import java.util.SortedMap;

/**
 * Base implementation for ships (player or NPC).
 */
public abstract class BaseArtemisShip extends BaseArtemisShielded {
    private float mShieldsFrontMax = Float.NaN;
    private float mShieldsRearMax = Float.NaN;
    private float mImpulse = Float.NaN;
    private byte mSide = -1;

    public BaseArtemisShip(int objId) {
        super(objId);
    }

    /**
     * The maximum strength of the forward shield.
     * Unspecified: Float.NaN
     */
    public float getShieldsFrontMax() {
        return mShieldsFrontMax;
    }

    public void setShieldsFrontMax(float shieldsFrontMax) {
        this.mShieldsFrontMax = shieldsFrontMax;
    }
    
    /**
     * The maximum strength of the aft shield.
     * Unspecified: Float.NaN
     */
    public float getShieldsRearMax() {
        return mShieldsRearMax;
    }

    public void setShieldsRearMax(float shieldsRearMax) {
        this.mShieldsRearMax = shieldsRearMax;
    }

    /**
     * Impulse setting, as a value from 0 (all stop) and 1 (full impulse).
     * Unspecified: -1
     */
    public float getImpulse() {
        return mImpulse;
    }

    public void setImpulse(float impulseSlider) {
        mImpulse = impulseSlider;
    }
    
    /**
     * The side this ship is on. There is no side 0. Biomechs are side 30.
     */
    public byte getSide() {
    	return mSide;
    }
    
    public void setSide(byte side) {
    	mSide = side;
    }

    @Override
    public void updateFrom(ArtemisObject obj) {
        super.updateFrom(obj);
        
        if (obj instanceof BaseArtemisShip) {
            BaseArtemisShip ship = (BaseArtemisShip) obj;
            
            if (!Float.isNaN(ship.mShieldsFrontMax)) mShieldsFrontMax = ship.mShieldsFrontMax;
            if (!Float.isNaN(ship.mShieldsRearMax)) mShieldsRearMax = ship.mShieldsRearMax;
            if (!Float.isNaN(ship.mImpulse)) setImpulse(ship.mImpulse);
            if (ship.mSide != -1) mSide = ship.mSide;
        }
    }

    @Override
	public void appendObjectProps(SortedMap<String, Object> props) {
    	super.appendObjectProps(props);
    	putProp(props, "Shields: fore max", mShieldsFrontMax, Float.NaN);
    	putProp(props, "Shields: aft max", mShieldsRearMax, Float.NaN);
    	putProp(props, "Impulse", mImpulse, Float.NaN);
    	putProp(props, "Side", mSide, -1);
    }
    
    /**
     * Returns true if this object has any data.
     */
    @Override
    protected boolean hasData() {
    	if (super.hasData()) return true;
    	
    	return  !Float.isNaN(mShieldsFrontMax) ||
    			!Float.isNaN(mShieldsRearMax) ||
    			!Float.isNaN(mImpulse) ||
    			mSide != 1;
    }
}