package com.walkertribe.ian.protocol.core;

/**
 * Defines the known packet types for the core Artemis protocol.
 * @author rjwut
 */
public final class CorePacketType {
	public static final String COMMS_MESSAGE = "commsMessage";
	public static final String COMM_TEXT = "commText";
	public static final String CONNECTED = "connected";
	public static final String OBJECT_BIT_STREAM = "objectBitStream";
	public static final String OBJECT_DELETE = "objectDelete";
	public static final String PLAIN_TEXT_GREETING = "plainTextGreeting";
	public static final String SIMPLE_EVENT = "simpleEvent";
	public static final String START_GAME = "startGame";
	public static final String VALUE_INT = "valueInt";

	/**
	 * No instantiation allowed.
	 */
	private CorePacketType() { }
}