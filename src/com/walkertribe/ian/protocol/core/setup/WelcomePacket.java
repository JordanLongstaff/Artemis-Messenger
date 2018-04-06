package com.walkertribe.ian.protocol.core.setup;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.util.Util;

/**
 * Sent by the server immediately on connection. The receipt of this packet
 * indicates a successful connection to the server.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.PLAIN_TEXT_GREETING)
public class WelcomePacket extends BaseArtemisPacket {
	protected static final String MSG =
			"You have connected to Thom Robertson's Artemis Bridge Simulator.  " +
			"Please connect with an authorized game client.";

	private String msg;

	public WelcomePacket(PacketReader reader) {
		msg = reader.readUsAsciiString();
	}

	/**
	 * Uses the default message given by the Artemis server on connection.
	 */
	public WelcomePacket() {
		msg = MSG;
	}
	
	/**
	 * Uses an arbitrary provided message.
	 */
	public WelcomePacket(String m) {
		if (Util.isBlank(m))
			throw new IllegalArgumentException("Missing welcome message");
		msg = m;
	}

	@Override
	protected void writePayload(PacketWriter writer) {
		writer.writeUsAsciiString(msg);
	}

	/**
	 * Returns the welcome message sent by the server.
	 */
	public String getMessage() {
		return msg;
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append(getMessage());
	}
}