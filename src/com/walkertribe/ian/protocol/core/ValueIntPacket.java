package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.protocol.Packet;

/**
 * A superclass for handling VALUE_INT client packets. Note that some packets
 * in the Artemis protocol technically have the valueInt type, but don't
 * actually follow the pattern of having a single int value. It may be that the
 * packets in question evolved over time and needed more values. Those packets
 * do not extend ValueIntPacket but are still mentioned in the SubType class.
 * @author rjwut
 */
public abstract class ValueIntPacket extends BaseArtemisPacket {
    /**
     * VALUE_INT client packet subtypes.
     */
    public static class Subtype {
    	public static final byte SET_SHIP                = 0x0d;
    	public static final byte SET_CONSOLE             = 0x0e;
    	public static final byte READY                   = 0x0f;

    	/**
    	 * No instantiation allowed.
    	 */
    	private Subtype() { }
    }

    protected final byte mSubtype;
    protected final int mArg;

    /**
     * Use this constructor if the class services only one subtype.
     */
    public ValueIntPacket(int arg) {
        mSubtype = getClass().getAnnotation(Packet.class).subtype()[0];
        mArg = arg;
    }

    /**
     * Use this constructor if the class services multiple subtypes.
     */
    public ValueIntPacket(byte subType, int arg) {
        mSubtype = subType;
        mArg = arg;
    }

    protected ValueIntPacket(PacketReader reader) {
    	mSubtype = (byte) reader.readInt();
    	mArg = reader.readInt();
    }

    @Override
	protected void writePayload(PacketWriter writer) {
		writer.writeInt(mSubtype);
		writer.writeInt(mArg);
	}
}