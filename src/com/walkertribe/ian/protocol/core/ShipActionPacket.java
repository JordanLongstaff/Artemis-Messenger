package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;

/**
 * A superclass for handling all type 0x4c821d3c client packets.
 * @author rjwut
 */
public abstract class ShipActionPacket extends BaseArtemisPacket {
    protected static final int TYPE = 0x4c821d3c;
    
    protected static final byte TYPE_SET_SHIP           = 0x0d;
    protected static final byte TYPE_SET_CONSOLE        = 0x0e;
    protected static final byte TYPE_READY              = 0x0f;

    private byte mSubType;
    protected int mArg = -1;

    /**
     * Use this constructor if you intend to override writePayload() with your
     * own implementation and not call ShipActionPacket.writePayload().
     * @param subType The desired packet subtype
     */
    public ShipActionPacket(byte subType) {
        super(ConnectionType.CLIENT, TYPE);
        mSubType = subType;
    }

    /**
     * Use this constructor if the packet has a single int argument that is
     * written to the payload after the subtype. In this case, you will not need
     * to override writePayload().
     * @param subType The desired packet subtype
     * @param arg A single argument to write to the payload after the subtype
     */
    public ShipActionPacket(byte subType, int arg) {
        this(subType);
        mArg = arg;
    }

    protected ShipActionPacket(byte subType, PacketReader reader) {
    	this(subType);
    	reader.skip(4); // subtype
    	mArg = reader.readInt();
    }

    @Override
	protected void writePayload(PacketWriter writer) {
		writer.writeInt(mSubType);
		writer.writeInt(mArg);
	}
}