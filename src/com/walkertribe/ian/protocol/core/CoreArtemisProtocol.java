package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.protocol.AbstractProtocol;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.core.comm.*;
import com.walkertribe.ian.protocol.core.setup.*;
import com.walkertribe.ian.protocol.core.world.*;

/**
 * Implements the core Artemis protocol.
 * @author rjwut
 */
public class CoreArtemisProtocol extends AbstractProtocol {
	// The packet classes supported by this Protocol
	private static final Class<?>[] CLASSES = {
			// server classes
			AllShipSettingsPacket.class,
			CommsIncomingPacket.class,
			DestroyObjectPacket.class,
			DockedPacket.class,
			EndGamePacket.class,
			GameOverReasonPacket.class,
			ObjectUpdatePacket.class,
			PausePacket.class,
			VersionPacket.class,
			WelcomePacket.class,

			// client classes
			CommsOutgoingPacket.class,
			ReadyPacket.class,
			SetConsolePacket.class,
			SetShipPacket.class
	};

	public CoreArtemisProtocol() {
		for (Class<?> clazz: CLASSES)
			register(clazz.asSubclass(ArtemisPacket.class));
	}
}