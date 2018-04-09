package com.walkertribe.ian.protocol.core.world;

import java.util.EnumSet;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.world.ArtemisPlayer;

/**
 * ObjectParser implementation for player ships
 * @author rjwut
 */
public class PlayerShipParser extends AbstractObjectParser {
	private enum Bit {
    	WEAPONS_TARGET,
    	IMPULSE,
    	RUDDER,
    	TOP_SPEED,
    	TURN_RATE,
    	TARGETING_MODE,
    	WARP,
    	ENERGY,

    	SHIELD_STATE,
    	SHIP_NUMBER,
    	SHIP_TYPE,
    	X,
    	Y,
    	Z,
    	PITCH,
    	ROLL,

    	HEADING,
    	VELOCITY,
    	NEBULA_TYPE,
    	NAME,
    	FORE_SHIELDS,
    	FORE_SHIELDS_MAX,
    	AFT_SHIELDS,
    	AFT_SHIELDS_MAX,

    	DOCKING_BASE,
    	ALERT_STATUS,
    	UNK_4_3,
    	MAIN_SCREEN,
    	BEAM_FREQUENCY,
    	COOLANT_OR_MISSILES,
    	SCIENCE_TARGET,
    	CAPTAIN_TARGET,

    	DRIVE_TYPE,
    	SCAN_OBJECT_ID,
    	SCAN_PROGRESS,
    	REVERSE_STATE,
    	UNK_5_5,
    	SIDE,
    	UNK_5_7,
    	SHIP_INDEX,

    	CAPITAL_SHIP_ID,
    	ACCENT_COLOR,
    	JUMP_TIME,
    	BEACON_CREATURE_TYPE,
    	BEACON_MODE
    }
	private static final EnumSet<Bit> BITS = EnumSet.allOf(Bit.class);
	
	private static final Version FIGHTER_VERSION = new Version("2.3.0");
	private static final Version COLOR_VERSION = new Version("2.4.0");
	private static final Version BEACON_VERSION = new Version("2.6.3");
	private static final Version NEBULA_VERSION = new Version("2.7.0");

    protected PlayerShipParser() {
		super(ObjectType.PLAYER_SHIP);
	}

	@Override
	public int getBitCount() {
		return BITS.size();
	}

	@Override
	protected ArtemisPlayer parseImpl(PacketReader reader) {
        ArtemisPlayer player = new ArtemisPlayer(reader.getObjectId());
        reader.readInt(Bit.WEAPONS_TARGET, -1);
        player.setImpulse(reader.readFloat(Bit.IMPULSE));
        reader.readFloat(Bit.RUDDER);
        reader.readFloat(Bit.TOP_SPEED);
        reader.readFloat(Bit.TURN_RATE);
        reader.readByte(Bit.TARGETING_MODE, (byte) 0);

        player.setWarp(reader.readByte(Bit.WARP, (byte) -1));
        reader.readFloat(Bit.ENERGY);
        reader.readBool(Bit.SHIELD_STATE, 2);

        player.setShipIndex((byte) (reader.readInt(Bit.SHIP_NUMBER, 0) - 1));
        player.setHullId(reader.readInt(Bit.SHIP_TYPE, -1));
        player.setX(reader.readFloat(Bit.X));
        player.setY(reader.readFloat(Bit.Y));
        player.setZ(reader.readFloat(Bit.Z));
        reader.readFloat(Bit.PITCH);
        reader.readFloat(Bit.ROLL);
        reader.readFloat(Bit.HEADING);
        reader.readFloat(Bit.VELOCITY);

        reader.readByte(Bit.NEBULA_TYPE, (byte) 0);
        if (reader.getVersion().lt(NEBULA_VERSION))
        	reader.readByte(Bit.NEBULA_TYPE, (byte) 0);
        
        player.setName(reader.readString(Bit.NAME));
        player.setShieldsFront(reader.readFloat(Bit.FORE_SHIELDS));
        player.setShieldsFrontMax(reader.readFloat(Bit.FORE_SHIELDS_MAX));
        player.setShieldsRear(reader.readFloat(Bit.AFT_SHIELDS));
        player.setShieldsRearMax(reader.readFloat(Bit.AFT_SHIELDS_MAX));
        player.setDockingBase(reader.readInt(Bit.DOCKING_BASE, 0));

        reader.readByte(Bit.ALERT_STATUS, (byte) 0);
        reader.readObjectUnknown(Bit.UNK_4_3, 4);
        reader.readByte(Bit.MAIN_SCREEN, (byte) 0);
        reader.readByte(Bit.BEAM_FREQUENCY, (byte) 0);
        reader.readByte(Bit.COOLANT_OR_MISSILES, (byte) -1);
        reader.readInt(Bit.SCIENCE_TARGET, -1);
        reader.readInt(Bit.CAPTAIN_TARGET, -1);
        reader.readByte(Bit.DRIVE_TYPE, (byte) 0);

        reader.readInt(Bit.SCAN_OBJECT_ID, -1);
        reader.readFloat(Bit.SCAN_PROGRESS);
        reader.readBool(Bit.REVERSE_STATE, 1);

        reader.readObjectUnknown(Bit.UNK_5_5, 4);
        player.setSide(reader.readByte(Bit.SIDE, (byte) -1));
        reader.readObjectUnknown(Bit.UNK_5_7, 4);
        
        if (BITS.contains(Bit.SHIP_INDEX)) {
        	byte shipIndex = player.getShipIndex();
	        player.setShipIndex(reader.readByte(Bit.SHIP_INDEX, (byte) -1));
	        if (player.getShipIndex() < 0) player.setShipIndex(shipIndex);
	        
	        reader.readInt(Bit.CAPITAL_SHIP_ID, -1);

	        if (BITS.contains(Bit.ACCENT_COLOR)) {
		        reader.readFloat(Bit.ACCENT_COLOR);
		        reader.readFloat(Bit.JUMP_TIME);

		        if (BITS.contains(Bit.BEACON_CREATURE_TYPE)) {
		            reader.readByte(Bit.BEACON_CREATURE_TYPE, (byte) 0);
		            reader.readByte(Bit.BEACON_MODE, (byte) 0);
		        }
	        }
        }
        
        return player;
	}
	
	@Override
	public void reconcile(Version version) {
		if (version.lt(FIGHTER_VERSION)) {
			BITS.remove(Bit.SHIP_INDEX);
			BITS.remove(Bit.CAPITAL_SHIP_ID);
		} else {
			BITS.add(Bit.SHIP_INDEX);
			BITS.add(Bit.CAPITAL_SHIP_ID);
		}
		
		if (version.lt(COLOR_VERSION)) {
			BITS.remove(Bit.ACCENT_COLOR);
			BITS.remove(Bit.JUMP_TIME);
		} else {
			BITS.add(Bit.ACCENT_COLOR);
			BITS.add(Bit.JUMP_TIME);
		}
		
		if (version.lt(BEACON_VERSION)) {
			BITS.remove(Bit.BEACON_CREATURE_TYPE);
			BITS.remove(Bit.BEACON_MODE);
		} else {
			BITS.add(Bit.BEACON_CREATURE_TYPE);
			BITS.add(Bit.BEACON_MODE);
		}
	}
}