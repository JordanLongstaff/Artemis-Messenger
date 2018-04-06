package com.walkertribe.ian.protocol.core.setup;

import com.walkertribe.ian.enums.Console;
import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.protocol.core.ValueIntPacket;

/**
 * Take or relinquish a bridge console.
 * @author dhleong
 */
@Packet(origin = Origin.CLIENT, type = CorePacketType.VALUE_INT, subtype = ValueIntPacket.Subtype.SET_CONSOLE)
public class SetConsolePacket extends ValueIntPacket {
	private Console mConsole;
	private boolean mSelected;

	/**
	 * @param console The Console being updated
	 * @param selected Whether the player is taking this console or not
	 */
	public SetConsolePacket(Console console, boolean selected) {
		super(getConsoleIndex(console));
        mConsole = console;
        mSelected = selected;
    }

	public SetConsolePacket(PacketReader reader) {
        super(reader);
		mConsole = Console.values()[mArg];
		mSelected = reader.readInt() == 1;
	}
	
	/**
	 * Gets the index of the Console if it is not null; otherwise, throws an IllegalArgumentException.
	 */
	private static int getConsoleIndex(Console console) {
        if (console == null)
        	throw new IllegalArgumentException("No console specified");
        return console.ordinal();
	}
	
	/**
	 * The Console being updated
	 */
	public Console getConsole() {
		return mConsole;
	}
	
	/**
	 * Returns true if the Console is selected
	 */
	public boolean isSelected() {
		return mSelected;
	}

	@Override
    public void writePayload(PacketWriter writer) {
		super.writePayload(writer);
    	writer.writeInt(mSelected ? 1 : 0);
    }

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append(mConsole).append(' ').append(mSelected ? "" : "de").append("selected");
	}
}