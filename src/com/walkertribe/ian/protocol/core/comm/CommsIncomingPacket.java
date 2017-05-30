package com.walkertribe.ian.protocol.core.comm;

import java.util.EnumSet;

import com.walkertribe.ian.enums.CommsSubjectType;
import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.BaseArtemisPacket;

/**
 * Received when an incoming COMMs message arrives.
 */
public class CommsIncomingPacket extends BaseArtemisPacket {
	public static final int MIN_PRIORITY_VALUE = 0;
	public static final int MAX_PRIORITY_VALUE = 8;

	private static final int TYPE = 0xD672C35F;

	public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.SERVER, TYPE, new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return CommsIncomingPacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new CommsIncomingPacket(reader);
			}
		});
	}

    private final int mPriority;
    private final String mFrom;
    private final String mMessage;
    private final EnumSet<CommsSubjectType> mFilters;

    private CommsIncomingPacket(PacketReader reader) {
        super(ConnectionType.SERVER, TYPE);
        
        mPriority = reader.readShort();
        
        if (reader.peekByte() == 0) {
        	mFilters = EnumSet.noneOf(CommsSubjectType.class);
        } else {
        	mFilters = CommsSubjectType.fromBits(mPriority);
        }
        
        mFrom = reader.readString();
        mMessage = reader.readString().replace('^', '\n');
    }

    public CommsIncomingPacket(int priority, String from, String message, int filterBits) {
    	super(ConnectionType.SERVER, TYPE);

    	if (filterBits == 0) {
	    	if (priority < MIN_PRIORITY_VALUE || priority > MAX_PRIORITY_VALUE) {
	    		throw new IllegalArgumentException("Invalid priority: " + priority);
	    	}
    	}

    	if (from == null || from.length() == 0) {
    		throw new IllegalArgumentException("You must provide a sender name");
    	}

    	if (message == null || message.length() == 0) {
    		throw new IllegalArgumentException("You must provide a message");
    	}

    	mPriority = priority;
    	mFrom = from;
    	mMessage = message;
    	mFilters = CommsSubjectType.fromBits(filterBits);
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
    public String getFrom() {
        return mFrom;
    }

    /**
     * The content of the message.
     */
    public String getMessage() {
        return mMessage;
    }
    
    /**
     * The filters applicable to the message.
     */
    public EnumSet<CommsSubjectType> getFilters() {
    	return mFilters;
    }

	@Override
	protected void writePayload(PacketWriter writer) {
		writer.writeInt(mPriority);
		writer.writeString(mFrom);
		writer.writeString(mMessage.replace('\n', '^'));
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append("from ").append(mFrom).append(": ").append(mMessage);
	}
}