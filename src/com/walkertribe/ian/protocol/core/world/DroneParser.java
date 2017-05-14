package com.walkertribe.ian.protocol.core.world;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.world.ArtemisDrone;
import com.walkertribe.ian.world.ArtemisObject;

public class DroneParser extends AbstractObjectParser {
    private enum Bit {
    	UNK_1_1,
    	X,
    	UNK_1_3,
    	Z,
    	UNK_1_5,
    	Y,
    	HEADING,
    	UNK_1_8
    }
    private static final Bit[] BITS = Bit.values();
    private static final byte[] ZERO_ARR = { (byte) 0 };

    DroneParser() {
		super(ObjectType.DRONE);
	}

	@Override
	public Bit[] getBits() {
		return BITS;
	}

	@Override
	protected ArtemisDrone parseImpl(PacketReader reader) {
        final ArtemisDrone drone = new ArtemisDrone(reader.getObjectId());
        reader.readObjectUnknown("UNK", 1);
    	reader.readObjectUnknown(Bit.UNK_1_1, 4);
    	drone.setX(reader.readFloat(Bit.X, Float.MIN_VALUE));
    	reader.readObjectUnknown(Bit.UNK_1_3, 4);
    	drone.setZ(reader.readFloat(Bit.Z, Float.MIN_VALUE));
    	reader.readObjectUnknown(Bit.UNK_1_5, 4);
    	drone.setY(reader.readFloat(Bit.Y, Float.MIN_VALUE));
    	drone.setHeading(reader.readFloat(Bit.HEADING, Float.MIN_VALUE));
    	reader.readObjectUnknown(Bit.UNK_1_8, 4);
        return drone;
	}

	@Override
	public void write(ArtemisObject obj, PacketWriter writer) {
		ArtemisDrone drone = (ArtemisDrone) obj;
		writer	.writeUnknown("UNK", ZERO_ARR)
				.writeUnknown(Bit.UNK_1_1)
				.writeFloat(Bit.X, drone.getX(), Float.MIN_VALUE)
				.writeUnknown(Bit.UNK_1_3)
				.writeFloat(Bit.Z, drone.getZ(), Float.MIN_VALUE)
				.writeUnknown(Bit.UNK_1_5)
				.writeFloat(Bit.Y, drone.getY(), Float.MIN_VALUE)
				.writeFloat(Bit.HEADING, drone.getHeading(), Float.MIN_VALUE)
				.writeUnknown(Bit.UNK_1_8);
	}
}