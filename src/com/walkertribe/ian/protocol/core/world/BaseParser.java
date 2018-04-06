package com.walkertribe.ian.protocol.core.world;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.world.ArtemisBase;

/**
 * ObjectParser implementation for bases
 * @author rjwut
 */
public class BaseParser extends AbstractObjectParser {
	private enum Bit {
		NAME,
		FORE_SHIELDS,
		AFT_SHIELDS,
		INDEX,
		HULL_ID,
		X,
		Y,
		Z,

		UNK_2_1,
		UNK_2_2,
		UNK_2_3,
		UNK_2_4,
		UNK_2_5,
		UNK_2_6
	}
	private static final int BIT_COUNT = Bit.values().length;

	BaseParser() {
		super(ObjectType.BASE);
	}

	@Override
	public int getBitCount() {
		return BIT_COUNT;
	}

	@Override
	protected ArtemisBase parseImpl(PacketReader reader) {
        ArtemisBase base = new ArtemisBase(reader.getObjectId());
        base.setName(reader.readString(Bit.NAME));
        base.setShieldsFront(reader.readFloat(Bit.FORE_SHIELDS));
        base.setShieldsRear(reader.readFloat(Bit.AFT_SHIELDS));
        reader.readObjectUnknown(Bit.INDEX, 4);
        base.setHullId(reader.readInt(Bit.HULL_ID, -1));
        base.setX(reader.readFloat(Bit.X));
        base.setY(reader.readFloat(Bit.Y));
        base.setZ(reader.readFloat(Bit.Z));
        reader.readObjectUnknown(Bit.UNK_2_1, 4);
        reader.readObjectUnknown(Bit.UNK_2_2, 4);
        reader.readObjectUnknown(Bit.UNK_2_3, 4);
        reader.readObjectUnknown(Bit.UNK_2_4, 4);
        reader.readObjectUnknown(Bit.UNK_2_5, 1);
        reader.readObjectUnknown(Bit.UNK_2_6, 1);
        return base;
	}
	
	/**
	 * BaseParsers have nothing to reconcile.
	 */
	@Override
	public void reconcile(Version version) { }
}