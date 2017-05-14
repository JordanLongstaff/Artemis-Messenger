package com.walkertribe.ian.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * Handles reading various data types from a byte array and tracking the offset
 * pointer.
 */
public class ByteArrayReader {
	/**
	 * Reads the indicated number of bytes from the given InputStream wrapped by
	 * this object and stores them in the provided buffer. This method blocks
	 * until the desired number of bytes has been read or the stream closes.
	 */
	public static void readBytes(InputStream in, int byteCount, byte[] buffer)
			throws InterruptedException, IOException {
		if (byteCount > buffer.length) {
			throw new IllegalArgumentException("Requested " + byteCount +
					" byte(s) but buffer is only " + buffer.length + " byte(s)");
		}

		int totalBytesRead = 0;

		while (true) {
			int bytesRead = in.read(buffer, totalBytesRead, byteCount - totalBytesRead);

			if (bytesRead == -1) {
				throw new EOFException("Stream is closed");
			}

			totalBytesRead += bytesRead;

			if (totalBytesRead < byteCount) {
				Thread.sleep(1);
			} else {
				break;
			}
		}
	}

	/**
	 * Reads a short (coerced to an int) from the indicated location in the
	 * given byte array.
	 */
	public static int readShort(byte[] bytes, int offset) {
		return (0xff & (bytes[offset + 1] << 8)) | (0xff & bytes[offset]);
	}

	/**
	 * Reads an int from the indicated location in the given byte array.
	 */
	public static int readInt(byte[] bytes, int offset) {
		return	((0xff & bytes[offset + 3]) << 24) |
				((0xff & bytes[offset + 2]) << 16) |
				((0xff & bytes[offset + 1]) << 8) |
				(0xff & bytes[offset]);
	}

	/**
	 * Reads a float from the indicated location in the given byte array.
	 */
	public static float readFloat(byte[] bytes, int offset) {
		return Float.intBitsToFloat(readInt(bytes, offset));
	}

	private byte[] bytes;
	private int offset;

	/**
	 * Constructs a new ByteArrayReader that will read the bytes from the given
	 * array.
	 */
	public ByteArrayReader(byte[] bytes) {
		this.bytes = bytes;
	}

	/**
	 * Returns the number of unread bytes.
	 */
	public int getBytesLeft() {
		return bytes.length - offset;
	}

	/**
	 * Returns the next byte to be read from the array without moving the
	 * pointer.
	 */
	public byte peek() {
		return bytes[offset];
	}

	/**
	 * Skips the indicated number of bytes.
	 */
	public void skip(int byteCount) {
		offset += byteCount;
	}

	/**
	 * Returns the next byte.
	 */
	public byte readByte() {
		return bytes[offset++];
	}

	/**
	 * Returns the next given number of bytes.
	 */
	public byte[] readBytes(int byteCount) {
		byte[] readBytes = Arrays.copyOfRange(bytes, offset, offset + byteCount);
		offset += byteCount;
		return readBytes;
	}

	/**
	 * Reads the given number of bytes, then returns true if the first byte was
	 * 1 and false otherwise.
	 */
	public boolean readBoolean(int byteCount) {
		return readBytes(byteCount)[0] == 1;
	}

	/**
	 * Reads the given number of bytes, then returns BoolState.TRUE if the first
	 * byte was 1 and BoolState.FALSE otherwise.
	 */
	public BoolState readBoolState(int byteCount) {
		return BoolState.from(readBoolean(byteCount));
	}

	/**
	 * Reads a short value (two bytes) and returns it coerced to an int.
	 */
	public int readShort() {
		int value = readShort(bytes, offset);
		offset += 2;
		return value;
	}

	/**
	 * Reads and returns an int value (four bytes).
	 */
	public int readInt() {
		int value =	readInt(bytes, offset);
		offset += 4;
		return value;
	}

	/**
	 * Reads and returns a float value (four bytes).
	 */
	public float readFloat() {
		return Float.intBitsToFloat(readInt());
	}

	/**
	 * Reads and returns a BitField, presuming that the given enum values
	 * represent the bits it stores.
	 */
	public BitField readBitField(Enum<?>[] bits) {
		BitField bitField = new BitField(bits, bytes, offset);
		offset += bitField.getByteCount();
		return bitField;
	}

	/**
	 * Reads and returns a US ASCII encoded String().
	 */
	public String readUsAsciiString() {
		return readString(Util.US_ASCII, 1, false);
	}

	/**
	 * Reads and returns a UTF-16LE encoded String().
	 */
	public String readUtf16LeString() {
		return readString(Util.UTF16LE, 2, true);
	}

	/**
	 * Reads a String in the given Charset, assuming the indicated number of
	 * bytes per character.
	 */
	private String readString(Charset charset, int bytesPerChar, boolean nullTerminated) {
		int charCount = readInt();
		int byteCount = charCount * bytesPerChar;
		int nullLength = nullTerminated ? bytesPerChar : 0;
		int endOffset = offset + byteCount - nullLength;
		byte[] readBytes = Arrays.copyOfRange(bytes, offset, endOffset);
		offset += byteCount;
		int i = 0;

		// check for "early" null
		for ( ; i < readBytes.length; i += bytesPerChar) {
			boolean isNull = true;

			for (int j = 0; isNull && j < bytesPerChar; j++) {
				isNull = readBytes[i + j] == 0;
			}

			if (isNull) {
				break;
			}
		}

		if (i != readBytes.length) {
			readBytes = Arrays.copyOfRange(readBytes, 0, i);
		}

		return new String(readBytes, charset);
	}
}