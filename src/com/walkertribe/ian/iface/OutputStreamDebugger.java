package com.walkertribe.ian.iface;

import java.io.OutputStream;
import java.io.PrintStream;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.RawPacket;
import com.walkertribe.ian.util.TextUtil;

/**
 * Debugger implementation that simply writes all packets to an OutputStream.
 * @author rjwut
 */
public class OutputStreamDebugger implements Debugger {
	private final String name;
	private final PrintStream out, err;

	/**
	 * Convenience constructor for new OutputStreamDebugger("", out);
	 */
	public OutputStreamDebugger(OutputStream out, OutputStream err) {
		this("", out, err);
	}

	/**
	 * Creates an OutputStreamDebugger with the indicated name that writes to
	 * the given OutputStream. The name is prefixed to each event that is
	 * written to the stream so you can tell which Debugger is reporting it.
	 */
	public OutputStreamDebugger(String name, OutputStream out, OutputStream err) {
		this.name = name;
		this.out = toPrintStream(out);
		this.err = toPrintStream(err);
	}

	@Override
	public void onRecvPacketBytes(Origin origin, int pktType, byte[] payload) {
		printPacketBytes(false, origin, pktType, payload);
	}

	@Override
	public void onRecvParsedPacket(ArtemisPacket pkt) {
		out.println(name + "> " + pkt);
	}

	@Override
	public void onRecvUnparsedPacket(RawPacket pkt) {
		out.println(name + "< " + pkt);
	}

	@Override
	public void onSendPacket(ArtemisPacket pkt) {
		out.println(name + "< " + pkt);
	}

	@Override
	public void onSendPacketBytes(Origin connType, int pktType,
			byte[] payload) {
		printPacketBytes(true, connType, pktType, payload);
	}

	@Override
	public void onPacketParseException(ArtemisPacketException ex) {
		ex.printPacketDump(err);
		ex.printStackTrace(err);
	}

	@Override
	public void onPacketWriteException(ArtemisPacket pkt, Exception ex) {
		err.println(pkt);
		ex.printStackTrace(err);
	}

	@Override
	public void warn(String msg) {
		out.println((name.equals("") ? "" : (name + ": ")) + "WARNING: " + msg);
	}
	
	/**
	 * Returns the given OutputStream cast to or wrapped in a PrintStream.
	 */
	private PrintStream toPrintStream(OutputStream stream) {
		if (stream instanceof PrintStream) return (PrintStream) stream;
		else return new PrintStream(stream);
	}

	/**
	 * Writes the bytes for the given packet to the OutputStream.
	 */
	private void printPacketBytes(boolean send, Origin connType,
			int pktType, byte[] payload) {
		out.println(
				name + (send ? "< " : "> ") +
				TextUtil.intToHexLE(ArtemisPacket.HEADER) + " " +
				TextUtil.intToHexLE(payload.length + 24) + " " +
				TextUtil.intToHexLE(connType.toInt()) + " " +
				TextUtil.intToHexLE(0) + " " +
				TextUtil.intToHexLE(payload.length + 4) + " " +
				TextUtil.intToHexLE(pktType) + " " +
				TextUtil.byteArrayToHexString(payload)
		);
	}
}