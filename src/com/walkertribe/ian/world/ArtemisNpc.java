package com.walkertribe.ian.world;

import java.util.Set;
import java.util.SortedMap;

import com.walkertribe.ian.Context;
import com.walkertribe.ian.enums.SpecialAbility;
import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.enums.ShipSystem;
import com.walkertribe.ian.util.BoolState;
import com.walkertribe.ian.util.Util;
import com.walkertribe.ian.vesseldata.Faction;
import com.walkertribe.ian.vesseldata.FactionAttribute;
import com.walkertribe.ian.vesseldata.Vessel;

/**
 * An NPC ship; they may have special abilities, and can be scanned.
 * @author dhleong
 */
public class ArtemisNpc extends BaseArtemisShip {
    // scan levels... only 2 for now
    public static final byte SCAN_LEVEL_BASIC = 1;
    public static final byte SCAN_LEVEL_FULL  = 2;

    private byte mScanLevel = -1;
    private int mSpecial = -1, mSpecialState = -1;
    private BoolState mEnemy = BoolState.UNKNOWN;
    private BoolState mSurrendered = BoolState.UNKNOWN;
    private byte mFleetNumber = (byte) -1;
    private String mIntel;
    private final float[] mSysDamage = new float[8];

    public ArtemisNpc(int objId) {
        super(objId);

        for (int i = 0; i < 8; i++) {
        	mSysDamage[i] = -1;
        }
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

    /**
     * Returns BoolState.TRUE if this ship has surrendered, and BoolState.FALSE
     * if it hasn't.
     * Unspecified: BoolState.UNKNOWN
     */
    public BoolState isSurrendered() {
    	return mSurrendered;
    }

    public void setSurrendered(BoolState surrendered) {
    	mSurrendered = surrendered;
    }

    /**
     * Returns the ship's fleet number.
     * Unspecified: -1
     */
    public byte getFleetNumber() {
    	return mFleetNumber;
    }

    public void setFleetNumber(byte fleetNumber) {
    	mFleetNumber = fleetNumber;
    }

    /**
     * Returns a Set containing the SpecialAbility values that pertain to this
     * ship.
     * Unspecified: null
     */
    public Set<SpecialAbility> getSpecialAbilities() {
    	return mSpecial != -1 ? SpecialAbility.fromValue(mSpecial) : null;
    }

    /**
     * Returns true if this ship has the specified special ability and false if
     * it does not or if it is unknown whether it has it.
     */
    public boolean hasSpecialAbility(SpecialAbility ability) {
        return mSpecial != -1 && ability.on(mSpecial);
    }

    /**
     * Returns true if this ship is using the specified special ability and
     * false if it is not.
     */
    public boolean isUsingSpecialAbilty(SpecialAbility ability) {
        return mSpecialState != -1 && ability.on(mSpecialState);
    }

    public int getSpecialBits() {
    	return mSpecial;
    }

    /**
     * Sets the special ability bit field.
     * Unspecified: -1
     */
    public void setSpecialBits(int special) {
        mSpecial = special;
    }

    public int getSpecialStateBits() {
    	return mSpecialState;
    }

    /**
     * Sets the special state bit field (what abilities are being used).
     * Unspecified: -1
     */
    public void setSpecialStateBits(int special) {
        mSpecialState = special;
    }

    /**
     * The scan level for this ship.
     * Unspecified: -1
     */
    public byte getScanLevel() {
        return mScanLevel;
    }

    public void setScanLevel(byte scanLevel) {
        mScanLevel = scanLevel;
    }

    /**
     * Returns true if this ship has been scanned at the given level or higher;
     * false otherwise.
     */
    public boolean isScanned(byte scanLevel) {
        return mScanLevel >= scanLevel;
    }

    /**
     * The intel String for this ship.
     * Unspecified: null
     */
    public String getIntel() {
    	return mIntel;
    }

    public void setIntel(String intel) {
    	mIntel = intel;
    }

    /**
     * The percentage of damage sustained by a particular system, expressed as
     * a value between 0 and 1.
     * Unspecified: -1
     */
    public float getSystemDamage(ShipSystem sys) {
    	return mSysDamage[sys.ordinal()];
    }

    public void setSystemDamage(ShipSystem sys, float value) {
    	mSysDamage[sys.ordinal()] = value;
    }

    @Override
    public void updateFrom(ArtemisObject eng, Context ctx) {
        super.updateFrom(eng, ctx);
        
        // it SHOULD be an ArtemisNpc
        if (eng instanceof ArtemisNpc) {
            ArtemisNpc cast = (ArtemisNpc) eng;
            BoolState enemy = cast.isEnemy();

            if (BoolState.isKnown(enemy)) {
            	mEnemy = enemy;
            }

            BoolState surrendered = cast.isSurrendered();

            if (BoolState.isKnown(surrendered)) {
            	mSurrendered = surrendered;
            }

            if (cast.mFleetNumber != (byte) -1) {
            	setFleetNumber(cast.mFleetNumber);
            }

            if (cast.mScanLevel != -1) {
                setScanLevel(cast.mScanLevel);
            }

            boolean special = false;
            Vessel vessel = getVessel(ctx);

        	if (vessel != null) {
        		Faction faction = vessel.getFaction();
    			special = faction.is(FactionAttribute.HASSPECIALS);
        	}

            if (cast.mSpecial != -1) {
            	setSpecialBits(special ? cast.mSpecial : 0);
            }

            if (cast.mSpecialState != -1) {
                setSpecialStateBits(special ? cast.mSpecialState : 0);
            }

            if (cast.mIntel != null) {
            	setIntel(cast.mIntel);
            }

            for (int i = 0; i < mSysDamage.length; i++) {
            	float value = cast.mSysDamage[i];

            	if (value != -1) {
                    mSysDamage[i] = value;
            	}
            }
        }
    }

    @Override
	public void appendObjectProps(SortedMap<String, Object> props,
			boolean includeUnspecified) {
    	super.appendObjectProps(props, includeUnspecified);
    	putProp(props, "Scan level", mScanLevel, -1, includeUnspecified);

    	if (mSpecial != -1) {
    		String str = Util.enumSetToString(SpecialAbility.fromValue(mSpecial));
    		props.put("Specials", str != "" ? str : "NONE");
    	} else if (includeUnspecified) {
        	props.put("Specials", "UNKNOWN");
    	}

    	if (mSpecialState != -1) {
    		String str = Util.enumSetToString(SpecialAbility.fromValue(mSpecialState));
    		props.put("Specials active", str != "" ? str : "NONE");
    	} else if (includeUnspecified) {
        	props.put("Specials active", "UNKNOWN");
    	}

    	putProp(props, "Is enemy", mEnemy, includeUnspecified);
    	putProp(props, "Surrendered", mSurrendered, includeUnspecified);
    	putProp(props, "Fleet number", mFleetNumber, -1, includeUnspecified);
    	putProp(props, "Intel", mIntel, includeUnspecified);
    	ShipSystem[] systems = ShipSystem.values();

    	for (int i = 0; i < mSysDamage.length; i++) {
    		putProp(props, "Damage: " + systems[i], mSysDamage[i],
    				-1, includeUnspecified);
    	}
    }
}