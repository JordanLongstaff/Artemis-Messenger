package com.walkertribe.ian.protocol;

import java.io.PrintStream;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.util.TextUtil;

import android.util.Log;

/**
 * Thrown when IAN encounters a problem while attempting to read or write a
 * packet of a known type. Unknown packets don't throw this exception; IAN
 * creates UnknownPacket objects for them.
 */
public class ArtemisPacketException extends Exception {
    private static final long serialVersionUID = 6305993950844264082L;
    private Origin origin;
    private int packetType;
    private byte[] payload;

    /**
     * @param string A description of the problem
     */
    public ArtemisPacketException(String string) {
        super(string);
    }

    /**
     * @param t The exception that caused ArtemisPacketException to be thrown
     */
    public ArtemisPacketException(Throwable t) {
        super(t);
    }

    /**
     * @param string A description of the problem
     * @param origin The packet's Origin
     */
    public ArtemisPacketException(String string, Origin origin) {
    	super(string);
    	this.origin = origin;
    }

    /**
     * @param t The exception that caused ArtemisPacketException to be thrown
     * @param origin The packet's Origin
     * @param packetType The packet's type value
     */
    public ArtemisPacketException(Throwable t, Origin origin, int packetType) {
        this(t, origin, packetType, null);
    }

    /**
     * @param t The exception that caused ArtemisPacketException to be thrown
     * @param origin The packet's Origin
     * @param packetType The packet's type value
     * @param payload The packet's payload bytes
     */
    public ArtemisPacketException(Throwable t, Origin origin, int packetType, byte[] payload) {
        super(t);
        appendParseDetails(origin, packetType, payload);
    }

    /**
     * Returns the packet's Origin, or null if unknown.
     */
    public Origin getOrigin() {
    	return origin;
    }

    /**
     * Returns the type value for this packet, or 0 if unknown.
     */
    public int getPacketType() {
    	return packetType;
    }

    /**
     * Returns the payload for this packet, or null if unknown.
     */
    public byte[] getPayload() {
    	return payload;
    }
    
    /**
     * Adds the packet's Origin, type and payload to this exception.
     */
    public void appendParseDetails(Origin origin, int packetType, byte[] payload) {
    	this.origin = origin;
    	this.packetType = packetType;
    	this.payload = payload;
    }
    
    /**
     * Logs the packet bytes to LogCat.
     */
    public void logPacketDump() {
    	Log.e(origin.toString(), getPacketDumpMessage());
    }
    
    /**
     * Dumps the packet bytes to the given PrintStream.
     */
    public void printPacketDump(PrintStream err) {
    	err.println(origin + ": " + getPacketDumpMessage());
    }

    /**
     * Returns the message to dump from the packet bytes.
     */
    private String getPacketDumpMessage() {
    	return TextUtil.intToHex(packetType) + " " + (payload == null ? "" : TextUtil.byteArrayToHexString(payload));
    }
    
    /**
     * Convert the data in this exception to an Unknown Packet. An
     * IllegalStateException will occur if the Origin or payload is null.
     */
    public UnknownPacket toUnknownPacket() {
    	if (origin == null) throw new IllegalStateException("Unknown origin", this);
    	if (payload == null) throw new IllegalStateException("Unknown payload", this);
    	return new UnknownPacket(packetType, payload);
    }
}