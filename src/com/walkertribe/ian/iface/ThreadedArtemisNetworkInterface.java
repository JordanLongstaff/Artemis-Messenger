package com.walkertribe.ian.iface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.walkertribe.ian.enums.OrdnanceType;
import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.CompositeProtocol;
import com.walkertribe.ian.protocol.Protocol;
import com.walkertribe.ian.protocol.core.CoreArtemisProtocol;
import com.walkertribe.ian.protocol.core.setup.VersionPacket;
import com.walkertribe.ian.protocol.core.setup.WelcomePacket;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.vesseldata.Faction;

/**
 * Default implementation of ArtemisNetworkInterface. Kicks off a thread for
 * each stream.
 */
public class ThreadedArtemisNetworkInterface implements ArtemisNetworkInterface {
	private Origin recvType;
    private Origin sendType;
    private Protocol protocol = new CoreArtemisProtocol();
    private ListenerRegistry mListeners = new ListenerRegistry();
    private ReceiverThread mReceiveThread;
    private SenderThread mSendThread;
    private DisconnectEvent.Cause disconnectCause = DisconnectEvent.Cause.LOCAL_DISCONNECT;
    private Exception exception;
    private Debugger mDebugger = new BaseDebugger();

    /**
     * Prepares an outgoing client connection to an Artemis server. The send and
     * receive streams won't actually be opened until start() is called. This
     * constructor causes IAN to wait forever for a connection; a separate
     * constructor is provided for specifying a timeout.
     */
    public ThreadedArtemisNetworkInterface(String host, int port)
    		throws IOException {
    	this(host, port, 0);
    }

    /**
     * Prepares an outgoing client connection to an Artemis server. The send and
     * receive streams won't actually be opened until start() is called. The
     * timeoutMs value indicates how long (in milliseconds) IAN will wait for
     * the connection to be established before throwing an exception; 0 means
     * "wait forever."
     */
    public ThreadedArtemisNetworkInterface(String host, int port, int timeoutMs) throws IOException {
    	Socket skt = new Socket();
    	skt.connect(new InetSocketAddress(host, port), timeoutMs);
    	init(skt, Origin.SERVER);
    }

    private void init(Socket skt, Origin origin) throws IOException {
    	recvType = origin;
    	sendType = origin.opposite();
    	skt.setKeepAlive(true);
        mSendThread = new SenderThread(this, skt);
        mReceiveThread = new ReceiverThread(this, skt);
    }

    @Override
    public Origin getRecvType() {
    	return recvType;
    }

    @Override
    public Origin getSendType() {
    	return sendType;
    }

    @Override
	public void registerProtocol(Protocol prot) {
		if (protocol instanceof CompositeProtocol) {
			((CompositeProtocol) protocol).add(prot);
		} else {
			CompositeProtocol composite = new CompositeProtocol();
			composite.add(protocol);
			composite.add(prot);
			protocol = composite;
		}
	}

    @Override
    public void addListener(final Object listener) {
    	mListeners.register(listener);
    }

    /**
     * By default, IAN will attempt to parse any packet it receives for which
     * there is a registered interested listener. Known packet types that have
     * no listeners will be discarded without being parsed, and unknown packet
     * types will emit UnknownPackets.
     * 
     * If this is set to false, IAN will treat all incoming packets as
     * UnknownPackets. This is useful to simply capture the raw bytes for all
     * packets, without attempting to parse them.
     */
    public void setParsePackets(boolean parse) {
    	mReceiveThread.setParsePackets(parse);
    }
    
    @Override
    public void setTimeout(int timeout) throws SocketException {
    	mSendThread.mSkt.setSoTimeout(timeout);
    }

    @Override
    public void start() {
        if (!mReceiveThread.mStarted) {
            mReceiveThread.start();
        }

        if (!mSendThread.mStarted) {
            mSendThread.start();
        }
    }

    @Override
    public boolean isConnected() {
        return mSendThread.mConnected;
    }

    @Override
    public void send(final ArtemisPacket pkt) {
    	if (pkt.getOrigin() != sendType) {
    		throw new IllegalArgumentException(
    				"Can only send " + sendType + " packets"
    		);
    	}

    	mSendThread.offer(pkt);
    }

    @Override
    public void stop() {
        mReceiveThread.end();
        mSendThread.end();
    }

    /**
	 * Manages sending packets to the OutputStream.
	 */
	private static class SenderThread extends Thread {
        private final Socket mSkt;
        private final Queue<ArtemisPacket> mQueue = new ConcurrentLinkedQueue<ArtemisPacket>();
        private boolean mRunning = true;
        
        private final PacketWriter mWriter;
        private final ThreadedArtemisNetworkInterface mInterface;
        
        private boolean mConnected;
        private boolean mStarted;

        public SenderThread(final ThreadedArtemisNetworkInterface net, final Socket skt) throws IOException {
            mInterface = net;
            mSkt = skt;
            OutputStream output = new BufferedOutputStream(mSkt.getOutputStream());
            mWriter = new PacketWriter(output);
        }

        /**
         * Enqueues a packet to be sent.
         */
        public boolean offer(final ArtemisPacket pkt) {
        	return mQueue.offer(pkt);
        }

        @Override
        public void run() {
            mStarted = true;

            while (mRunning) {
                try {
                    Thread.sleep(5);
                } catch (final InterruptedException ex) {
                	// TODO Supposed to bail if an InterruptedException is received
                }

                ArtemisPacket pkt = mQueue.poll();

            	if (pkt == null) {
                    // empty queue; loop back to wait
                    continue;
                }

            	mInterface.mDebugger.onSendPacket(pkt);

            	try {
                    pkt.writeTo(mWriter, mInterface.mDebugger);
                } catch (final IOException ex) {
                    if (mRunning) {
                    	mInterface.disconnectCause = DisconnectEvent.Cause.IO_EXCEPTION;
                    	mInterface.exception = ex;
                    }

                    break;
                } catch (final Exception ex) {
                	mInterface.mDebugger.onPacketWriteException(pkt, ex);
                }
            }

            mConnected = false;
            mInterface.stop();
            
            // Close the socket here; this will allow us to send any closing
            // packets needed before shutting down the pipes.
            try {
                mSkt.close();
            } catch (final IOException ex) {
            	// DON'T CARE
            }

            mInterface.mListeners.fire(new DisconnectEvent(
            		mInterface.disconnectCause,
            		mInterface.exception
            ));
        }

        /**
         * Stop sending packets after the current one.
         */
        public void end() {
            mRunning = false;
        }

        /**
         * Receiving a WelcomePacket is how we know we're connected to the
         * server. Send a ConnectionSuccessEvent.
         */
        public void onPacket(final WelcomePacket pkt) {
            final boolean wasConnected = mConnected;
            mConnected = true;

            if (!wasConnected) {
            	mInterface.mListeners.fire(new ConnectionSuccessEvent());
            }
        }

        /**
         * Check the Version against our minimum required version and disconnect
         * if we don't support it.
         */
        public void onPacket(final VersionPacket pkt) {
            final Version version = pkt.getVersion();

            if (version.lt(ArtemisNetworkInterface.MIN_VERSION)) {
            	mInterface.mListeners.fire(new DisconnectEvent(
            			DisconnectEvent.Cause.UNSUPPORTED_SERVER_VERSION,
            			null
            	));
                
                // go ahead and end the receive thread NOW
                mInterface.mReceiveThread.end();
                end();
            }
            
            // Reconcile static class versions
            OrdnanceType.reconcile(version);
            Faction.setVersion(version);
        }
    }

	/**
	 * Manages receiving packets from the InputStream.
	 */
    private class ReceiverThread extends Thread {
        private boolean mRunning = true;
        private final ThreadedArtemisNetworkInterface mInterface;
        private PacketReader mReader;
        private boolean mStarted;
        
        public ReceiverThread(final ThreadedArtemisNetworkInterface net, final Socket skt) throws IOException {
            mInterface = net;
            InputStream input = new BufferedInputStream(skt.getInputStream());
            mReader = new PacketReader(net.getRecvType(), input, protocol, mListeners);
        }

        /**
         * If set to false, we won't bother to parse any packets.
         */
        private void setParsePackets(boolean parse) {
        	mReader.setParsePackets(parse);
        }

        @Override
        public void run() {
            mStarted = true;
            SenderThread sender = ThreadedArtemisNetworkInterface.this.mSendThread;
            
            while (mRunning) {
                try {
                    // read packet
                	final ParseResult result = mReader.readPacket(mInterface.mDebugger);
                	if (result.getException() != null) handlePacketException(result.getException());

                    if (mRunning) {
                        final ArtemisPacket pkt = result.getPacket();
                        
                    	// Handle WelcomePacket and VersionPacket specially
                    	if (pkt instanceof WelcomePacket) {
                    		sender.onPacket((WelcomePacket) pkt);
                    	} else if (pkt instanceof VersionPacket) {
                    		sender.onPacket((VersionPacket) pkt);
                    	}

                    	// Notify listeners
                    	result.fireListeners();
                    }
                } catch (final ArtemisPacketException ex) {
                	handlePacketException(ex);
                }
            }
            
            mInterface.stop();
        }
        
        /**
         * An exception occurred while parsing; inform the debugger, then
         * determine whether it was fatal. If it was, shut down the connection.
         * If it wasn't, pass the packet along to any proxy targets.
         */
        private void handlePacketException(ArtemisPacketException ex) {
        	mDebugger.onPacketParseException(ex);

        	if (mRunning && ex.getPayload() == null) {
        		// Exception is fatal; shut down connection
            	Throwable cause = ex.getCause();
            	
            	if (cause instanceof EOFException || cause instanceof SocketException ||
            			cause instanceof SocketTimeoutException) {
            		mInterface.disconnectCause = DisconnectEvent.Cause.REMOTE_DISCONNECT;
            	} else {
            		mInterface.disconnectCause = DisconnectEvent.Cause.PACKET_PARSE_EXCEPTION;
            	}

            	mInterface.exception = (Exception) cause;
            	end();
        	}
        }

        /**
         * Requests that the receiver thread is shut down.
         */
        public void end() {
            mRunning = false;
        }
    }
    
	@Override
	public void attachDebugger(Debugger debugger) {
		if (debugger == null) {
			debugger = new BaseDebugger();
		}

		mDebugger = debugger;
	}
}