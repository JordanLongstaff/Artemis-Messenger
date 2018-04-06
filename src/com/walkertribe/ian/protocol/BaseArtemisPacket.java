package com.walkertribe.ian.protocol;

import java.io.IOException;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.Debugger;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.util.JamCrc;

/**
 * Implements common packet functionality.
 */
public abstract class BaseArtemisPacket implements ArtemisPacket {
	/**
	 * Determines the packet type hash specified in the given Packet annotation.
	 */
	public static int getHash(Packet annotation) {
		String typeName = annotation.type();
		
		if (typeName.length() != 0)
			return JamCrc.compute(typeName);
		
		int type = annotation.hash();
		
		if (type == 0)
			throw new RuntimeException("@Packet must specify either a type or a hash");
		
		return type;
	}
	
	/**
	 * Causes the packet's payload to be written to the given PacketWriter.
	 */
    protected abstract void writePayload(PacketWriter writer);

    /**
     * Writes packet type-specific details (debug info) to be written to the
     * given StringBuilder.
     */
    protected abstract void appendPacketDetail(StringBuilder b);

    private final Origin mOrigin;
    private final int mType;
    private final long mTimestamp;

    public BaseArtemisPacket() {
        Packet annotation = getClass().getAnnotation(Packet.class);
        
        if (annotation != null) {
        	mOrigin = annotation.origin();
        	mType = getHash(annotation);
        } else throw new RuntimeException(getClass() + " must have a @Packet annotation");
        
        mTimestamp = System.nanoTime();
    }
    
    /**
     * Constructor used only for inheritance by RawPacket.
     */
    protected BaseArtemisPacket(Origin origin, int type) {
    	mOrigin = origin;
    	mType = type;
        mTimestamp = System.nanoTime();
    }

    @Override
    public Origin getOrigin() {
        return mOrigin;
    }

    @Override
    public int getType() {
        return mType;
    }
    
    @Override
    public long getTimestamp() {
    	return mTimestamp;
    }

    @Override
    public final void writeTo(PacketWriter writer, Debugger debugger) throws IOException {
    	writer.start(mOrigin, mType);
    	writePayload(writer);
    	writer.flush(debugger);
    }

    @Override
    public final String toString() {
    	StringBuilder b = new StringBuilder();
    	b.append('[').append(getClass().getSimpleName()).append("] ");
    	appendPacketDetail(b);
    	return b.toString();
    }
}