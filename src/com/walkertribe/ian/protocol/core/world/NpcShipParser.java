package com.walkertribe.ian.protocol.core.world;

import java.util.EnumSet;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.world.ArtemisNpc;

/**
 * ObjectParser implementation for NPC ships
 * @author rjwut
 */
public class NpcShipParser extends AbstractObjectParser {
	private enum Bit {
		NAME,
		IMPULSE,
		RUDDER,
		MAX_IMPULSE,
		MAX_TURN_RATE,
		IS_ENEMY,
		SHIP_TYPE,
		X,

		Y,
		Z,
		PITCH,
		ROLL,
		HEADING,
		VELOCITY,
		SURRENDERED,
		UNK_2_8,

		FORE_SHIELD,
		FORE_SHIELD_MAX,
		AFT_SHIELD,
		AFT_SHIELD_MAX,
		UNK_3_5,
		FLEET_NUMBER,
		SPECIAL_ABILITIES,
		SPECIAL_STATE,

		SINGLE_SCAN,
		DOUBLE_SCAN,
		VISIBILITY,
		SIDE,
		UNK_4_5,
		UNK_4_6,
		UNK_4_7,
		TARGET_X,

		TARGET_Y,
		TARGET_Z,
		TAGGED,
		UNK_5_4,
		BEAM_SYSTEM_DAMAGE,
		TORPEDO_SYSTEM_DAMAGE,
		SENSOR_SYSTEM_DAMAGE,
		MANEUVER_SYSTEM_DAMAGE,
		
		IMPULSE_SYSTEM_DAMAGE,
		WARP_SYSTEM_DAMAGE,
		FORE_SHIELD_SYSTEM_DAMAGE,
		AFT_SHIELD_SYSTEM_DAMAGE,
		SHIELD_FREQUENCY_A,
		SHIELD_FREQUENCY_B,
		SHIELD_FREQUENCY_C,
		SHIELD_FREQUENCY_D,
		
		SHIELD_FREQUENCY_E
	}
	private static final EnumSet<Bit> BITS = EnumSet.allOf(Bit.class);
	
	private static final Version TAG_VERSION = new Version("2.6.204");
	private static final Version UNK_VERSION = new Version("2.6.3");
	private static final Version CURRENT_VERSION = new Version("2.7.0");

	NpcShipParser() {
		super(ObjectType.NPC_SHIP);
	}

	@Override
	public int getBitCount() {
		return BITS.size();
	}

	@Override
	protected ArtemisNpc parseImpl(PacketReader reader) {
        ArtemisNpc obj = new ArtemisNpc(reader.getObjectId());
        obj.setName(reader.readString(Bit.NAME));
        obj.setImpulse(reader.readFloat(Bit.IMPULSE));
        
        reader.readFloat(Bit.RUDDER);
        reader.readFloat(Bit.MAX_IMPULSE);
        reader.readFloat(Bit.MAX_TURN_RATE);
        
        obj.setEnemy(reader.readBool(Bit.IS_ENEMY, 4));
        obj.setHullId(reader.readInt(Bit.SHIP_TYPE, -1));
        obj.setX(reader.readFloat(Bit.X));
        obj.setY(reader.readFloat(Bit.Y));
        obj.setZ(reader.readFloat(Bit.Z));
        
        reader.readFloat(Bit.PITCH);
        reader.readFloat(Bit.ROLL);
        reader.readFloat(Bit.HEADING);
        reader.readFloat(Bit.VELOCITY);
        reader.readBool(Bit.SURRENDERED, 1);
        reader.readObjectUnknown(Bit.UNK_2_8, reader.getVersion().lt(CURRENT_VERSION) ? 2 : 1);

        obj.setShieldsFront(reader.readFloat(Bit.FORE_SHIELD));
        obj.setShieldsFrontMax(reader.readFloat(Bit.FORE_SHIELD_MAX));
        obj.setShieldsRear(reader.readFloat(Bit.AFT_SHIELD));
        obj.setShieldsRearMax(reader.readFloat(Bit.AFT_SHIELD_MAX));

        reader.readObjectUnknown(Bit.UNK_3_5, 2);
        reader.readByte(Bit.FLEET_NUMBER, (byte) -1);
        reader.readInt(Bit.SPECIAL_ABILITIES, -1);
        reader.readInt(Bit.SPECIAL_STATE, -1);
        reader.readInt(Bit.SINGLE_SCAN, -1);
        reader.readInt(Bit.DOUBLE_SCAN, -1);
        reader.readInt(Bit.VISIBILITY, 0);
        
        obj.setSide(reader.readByte(Bit.SIDE, (byte) -1));
        
        reader.readObjectUnknown(Bit.UNK_4_5, 1);
        reader.readObjectUnknown(Bit.UNK_4_6, 1);
        reader.readObjectUnknown(Bit.UNK_4_7, 1);

        reader.readFloat(Bit.TARGET_X);
        reader.readFloat(Bit.TARGET_Y);
        reader.readFloat(Bit.TARGET_Z);
        
        if (BITS.contains(Bit.TAGGED)) reader.readBool(Bit.TAGGED, 1);
        if (BITS.contains(Bit.UNK_5_4)) reader.readObjectUnknown(Bit.UNK_5_4, 1);
        
        int adjustment = Bit.values().length - BITS.size();
        for (int i = Bit.BEAM_SYSTEM_DAMAGE.ordinal() - adjustment; i < BITS.size(); i++)
        	reader.readFloat(i);

        return obj;
	}
	
	@Override
	public void reconcile(Version version) {
		if (version.lt(TAG_VERSION)) BITS.remove(Bit.TAGGED);
		else BITS.add(Bit.TAGGED);
		
		if (version.lt(UNK_VERSION)) BITS.remove(Bit.UNK_5_4);
		else BITS.add(Bit.UNK_5_4);
	}
}