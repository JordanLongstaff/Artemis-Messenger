package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;

/**
 * Sends a keystroke to the server. This should only be done for the game master
 * console, or if keystroke capture has been enabled via the
 * KeyCaptureTogglePacket.
 * @author rjwut
 * @see {@link java.awt.event.KeyEvent} (for constants)
 */
public class KeystrokePacket extends ShipActionPacket {
	public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.CLIENT, TYPE, TYPE_KEYSTROKE,
				new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return KeystrokePacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new KeystrokePacket(reader);
			}
		});
	}

	public KeystrokePacket(int keycode) {
		super(TYPE_KEYSTROKE, keycode);
	}

	private KeystrokePacket(PacketReader reader) {
		super(TYPE_KEYSTROKE, reader);
	}

	/**
	 * Returns the keycode for the key that was pressed.
	 */
	public int getKeycode() {
		return mArg;
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append(mArg);
	}
}