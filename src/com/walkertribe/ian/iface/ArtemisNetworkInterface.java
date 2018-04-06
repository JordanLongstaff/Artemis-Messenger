package com.walkertribe.ian.iface;

import java.io.IOException;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.Protocol;
import com.walkertribe.ian.protocol.core.CoreArtemisProtocol;
import com.walkertribe.ian.util.Version;

/**
 * Interface for objects which can connect to an Artemis server and send and
 * receive packets.
 */
public interface ArtemisNetworkInterface {
	public static final Version MIN_VERSION = new Version("2.2");

	/**
     * Returns the ConnectionType of the packets this interface can receive. An
     * ArtemisProtocolException will be thrown if it receives a packet of the
     * wrong type.
     */
    public Origin getRecvType();

    /**
     * Returns the ConnectionType of the packets this interface can send. An
     * ArtemisProtocolException will be thrown if it is asked to send a packet
     * of the wrong type.
     */
    public Origin getSendType();

    /**
     * Registers the packet types defined by the given Protocol with this
     * object. The {@link CoreArtemisProtocol} is registered automatically.
     */
	public void registerProtocol(Protocol protocol);

    /**
     * Registers an object as a listener. It must have one or more qualifying
     * methods annotated with {@link Listener}.
     */
    public void addListener(Object listener);

    /**
     * Opens the send/receive streams to the remote machine.
     */
    public void start();

    /**
     * Returns true if currently connected to the remote machine; false
     * otherwise.
     */
    public boolean isConnected();

    /**
     * Enqueues a packet to be transmitted to the remote machine.
     */
    public void send(ArtemisPacket pkt);

    /**
     * Closes the connection to the remote machine.
     */
    public void stop();

    /**
     * Attaches the given debugger to the interface. Any previously attached
     * debugger is removed. If debugger is null, the previous debugger, if any,
     * is removed, with no new debugger attached.
     */
    public void attachDebugger(Debugger debugger);
    
    /**
     * Sets the timeout value.
     * @param timeout the timeout value
     */
    public void setTimeout(int timeout) throws IOException;
}
