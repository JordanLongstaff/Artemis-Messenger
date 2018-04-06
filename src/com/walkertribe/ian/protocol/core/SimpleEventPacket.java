package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.protocol.Packet;

/**
 * A superclass for handling simpleEvent server packets.
 * @author rjwut
 */
public abstract class SimpleEventPacket extends BaseArtemisPacket {
	/**
	 * simpleEvent server packet subtypes.
	 */
	public static final class Subtype {
		public static final byte PAUSE         = 0x04;
		public static final byte END_GAME      = 0x06;
		public static final byte SHIP_SETTINGS = 0x0f;
		public static final byte GAME_REASON   = 0x14;
		public static final byte DOCKED        = 0x1a;
		
		/**
		 * No instantiation allowed.
		 */
		private Subtype() { }
	}
	
	private byte mSubtype;
	
	/**
	 * Use this constructor if the class services only one subtype.
	 */
	protected SimpleEventPacket() {
		mSubtype = getClass().getAnnotation(Packet.class).subtype()[0];
	}
	
	/**
	 * Use this constructor if the class services multiple subtypes.
	 */
	protected SimpleEventPacket(byte subtype) {
		mSubtype = subtype;
	}
	
	protected SimpleEventPacket(PacketReader reader) {
		mSubtype = (byte) reader.readInt();
	}
	
	@Override
	protected void writePayload(PacketWriter writer) {
		writer.writeInt(mSubtype);
	}
}