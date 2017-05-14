package com.walkertribe.ian.protocol.core.weap;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.core.ShipActionPacket;

/**
 * Toggles auto beams on/off.
 * @author rjwut
 */
public class ToggleAutoBeamsPacket extends ShipActionPacket {
	public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.CLIENT, TYPE, TYPE_TOGGLE_AUTO_BEAMS,
				new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return ToggleAutoBeamsPacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new ToggleAutoBeamsPacket(reader);
			}
		});
	}

	public ToggleAutoBeamsPacket() {
		super(TYPE_TOGGLE_AUTO_BEAMS, 0);
	}

	private ToggleAutoBeamsPacket(PacketReader reader) {
		super(TYPE_TOGGLE_AUTO_BEAMS, reader);
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		// do nothing
	}
}