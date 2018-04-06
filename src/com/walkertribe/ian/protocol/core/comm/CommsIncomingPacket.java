package com.walkertribe.ian.protocol.core.comm;

import java.util.EnumSet;

import com.walkertribe.ian.enums.CommsFilter;
import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.util.Util;
import com.walkertribe.ian.util.Version;

/**
 * Received when an incoming COMMs message arrives.
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.COMM_TEXT)
public class CommsIncomingPacket extends BaseArtemisPacket {
	public static final int MIN_PRIORITY_VALUE = 0;
	public static final int MAX_PRIORITY_VALUE = 8;
	private static final Version FILTERS_VERSION = new Version("2.6.0");
	
    private final int mPriority;
    private final CharSequence mFrom;
    private final CharSequence mMessage;
    private final EnumSet<CommsFilter> mFilters;
    
    public CommsIncomingPacket(PacketReader reader) {
    	if (reader.getVersion().ge(FILTERS_VERSION)) {
    		mPriority = reader.readShort();
    		mFilters = CommsFilter.fromBits(mPriority);
    	} else {
    		mPriority = reader.readInt();
    		mFilters = EnumSet.noneOf(CommsFilter.class);
    	}
    	
    	mFrom = reader.readString();
    	mMessage = Util.caratToNewline(reader.readString());
    }

    public CommsIncomingPacket(int priority, CharSequence from, CharSequence message, int filterBits) {
    	if (filterBits == 0) {
	    	if (priority < MIN_PRIORITY_VALUE || priority > MAX_PRIORITY_VALUE)
	    		throw new IllegalArgumentException("Invalid priority: " + priority);
    	}

    	if (Util.isBlank(from))
    		throw new IllegalArgumentException("Incoming Comms packet with no sender");
    	if (Util.isBlank(message))
    		throw new IllegalArgumentException("Incoming Comms packet with no message");

    	mPriority = priority;
    	mFrom = from;
    	mMessage = message;
    	mFilters = CommsFilter.fromBits(filterBits);
    }

    /**
     * Returns the message priority, with lower values having higher priority.
     * @return An integer between 0 and 8, inclusive
     */
    public int getPriority() {
        return mPriority;
    }

    /**
     * A String identifying the sender. This may not correspond to the name of
     * a game entity. For example, some messages from bases or friendly ships
     * have additional detail after the entity's name ("DS3 TSN Base"). Messages
     * in scripted scenarios can have any String for the sender.
     */
    public CharSequence getFrom() {
        return mFrom;
    }

    /**
     * The content of the message.
     */
    public CharSequence getMessage() {
        return mMessage;
    }
    
    /**
     * The filters applicable to the message.
     */
    public EnumSet<CommsFilter> getFilters() {
    	return mFilters;
    }

	@Override
	protected void writePayload(PacketWriter writer) {
    	if (writer.getVersion().ge(FILTERS_VERSION)) {
    		writer.writeShort(mPriority);
    	} else {
    		writer.writeInt(mPriority);
    	}
		writer.writeString(mFrom);
		writer.writeString(Util.newlineToCarat(mMessage));
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append("from ").append(mFrom).append(": ").append(mMessage);
	}
}