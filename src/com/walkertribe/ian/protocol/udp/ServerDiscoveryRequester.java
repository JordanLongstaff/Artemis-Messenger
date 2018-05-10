package com.walkertribe.ian.protocol.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

/**
 * Searches for servers on the LAN. When run on a Thread, the requester will
 * broadcast out a request to discover servers, then listen for response for a
 * configurable amount of time. The requester can be reused to send out
 * requests again, which is needed to continually poll for servers.
 * @author rjwut
 */
public class ServerDiscoveryRequester implements Runnable {
	static final int PORT = 3100;
	private static final byte[] DATA = new byte[] { Server.ENQ };

	/**
	 * Interface for an object which is notified when a server is discovered or
	 * the discovery process ends.
	 */
	public interface Listener {
		/**
		 * Invoked when a server is discovered.
		 */
		public void onDiscovered(Server server);

		/**
		 * Invoked with the ServerDiscoveryRequester quits listening for responses.
		 */
		public void onQuit();
	}

	private Listener listener;
	private int timeoutMs;
	private DatagramSocket skt;
	private InetAddress broadcastAddr;
	private byte[] buffer = new byte[255];

	/**
	 * Finds an appropriate network interface from which to broadcast.
	 */
	public ServerDiscoveryRequester(Listener listener, int timeout) throws IOException {
		PrivateNetworkAddress addr = PrivateNetworkAddress.findOne();
		init(addr != null ? addr.getBroadcastAddress() : null, listener, timeout);
	}
	
	/**
	 * Broadcasts using the given InetAddress.
	 */
	public ServerDiscoveryRequester(InetAddress broadcast, Listener listener, int timeout) throws IOException {
		init(broadcast, listener, timeout);
	}
	
	/**
	 * Common initialization for both constructors.
	 */
	private void init(InetAddress broadcastAddress, Listener listener, int timeoutMs) throws IOException {
		if (listener == null) {
			throw new IllegalArgumentException("You must provide a listener");
		}

		if (timeoutMs < 1) {
			throw new IllegalArgumentException("Invalid timeout: " + timeoutMs);
		}

		this.listener = listener;
		this.timeoutMs = timeoutMs;
		this.broadcastAddr = broadcastAddress;
		if (this.broadcastAddr == null)
			this.broadcastAddr = InetAddress.getByName("255.255.255.255");
	}

	@Override
	public synchronized void run() {
		try {
			// Broadcast
			skt = new DatagramSocket();
			skt.setBroadcast(true);
			skt.send(new DatagramPacket(DATA, 1, broadcastAddr, PORT));
			
			// Listen for responses
			long endTime = System.currentTimeMillis() + timeoutMs;
			do {
				int timeLeft = Math.max((int) (endTime - System.currentTimeMillis()), 1);
				if (timeLeft < 1) break;
				
				skt.setSoTimeout(timeLeft);
				DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);

				try {
					skt.receive(pkt);
					byte[] data = pkt.getData();

					if (data[0] != Server.ACK) {
						continue; // didn't get ACK; ignore
					}

					listener.onDiscovered(Server.from(pkt.getData()));
				} catch (SocketTimeoutException ex) {
					break;
				}
			} while (true);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			skt.close();
			skt = null;
			listener.onQuit();
		}
	}
}
