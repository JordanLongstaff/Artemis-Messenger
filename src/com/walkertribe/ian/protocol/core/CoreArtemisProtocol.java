package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.protocol.AbstractProtocol;
import com.walkertribe.ian.protocol.core.comm.*;
import com.walkertribe.ian.protocol.core.setup.*;
import com.walkertribe.ian.protocol.core.world.*;

/**
 * Implements the core Artemis protocol.
 * @author rjwut
 */
public class CoreArtemisProtocol extends AbstractProtocol {
	// The packet classes supported by this Protocol
	private static final Class<?>[] PACKET_CLASSES = {
			// server classes
			// -- prioritized
			ObjectUpdatePacket.class,
			IntelPacket.class,
			// -- rest
			AllShipSettingsPacket.class,
			CommsIncomingPacket.class,
			ConsoleStatusPacket.class,
			DestroyObjectPacket.class,
			GameOverPacket.class,
			GameOverReasonPacket.class,
			HeartbeatPacket.class,
			PausePacket.class,
			VersionPacket.class,
			WelcomePacket.class,

			// client classes
			// -- prioritized
			// -- rest
			CommsOutgoingPacket.class,
			ReadyPacket.class,
			SetShipPacket.class,
			SetConsolePacket.class
	};

	@Override
	public void registerPacketFactories(PacketFactoryRegistry registry) {
		registerPacketFactories(registry, PACKET_CLASSES);
	}
}