package com.walkertribe.ian.protocol;

import java.io.IOException;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.Debugger;
import com.walkertribe.ian.iface.PacketWriter;

/**
 * Interface for all packets that can be received or sent.
 */
public interface ArtemisPacket {
	/**
	 * The preamble of every packet starts with this value.
	 */
	public static final int HEADER = 0xdeadbeef;

    /**
     * Returns a ConnectionType value indicating the type of connection from
     * which this packet originates. SERVER means that this packet type is sent
     * by the server; CLIENT means it's sent by the client.
     */
    public ConnectionType getConnectionType();

    /**
     * Returns the type value for this packet, specified as the last field of
     * the preamble.
     */
    public int getType();

    /**
     * Writes this packet to the given PacketWriter, then returns the array of
     * bytes that was sent. The given Debugger will be notified of the written
     * packet.
     */
    public void writeTo(PacketWriter writer, Debugger debugger) throws IOException;
}