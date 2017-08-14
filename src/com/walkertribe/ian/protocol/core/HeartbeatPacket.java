package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.BaseArtemisPacket;

public class HeartbeatPacket extends BaseArtemisPacket {
	private static final int TYPE = 0xf5821226;
	
	public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.SERVER, TYPE,
				new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return HeartbeatPacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new HeartbeatPacket();
			}
		});
	}

	public HeartbeatPacket() throws ArtemisPacketException {
		super(ConnectionType.SERVER, TYPE);
	}

	@Override
	protected void writePayload(PacketWriter writer) { }

	@Override
	protected void appendPacketDetail(StringBuilder b) { }
}