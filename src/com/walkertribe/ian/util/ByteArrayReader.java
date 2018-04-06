package com.walkertribe.ian.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

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
		if (byteCount == 0) return;

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
		return ((0xff & bytes[offset + 1]) << 8) | (0xff & bytes[offset]);
	}

	/**
	 * Reads an int from the indicated location in the given byte array.
	 */
	public static int readInt(byte[] bytes, int offset) {
		int read = 0;
		for (int i = 0; i < 4; i++) {
			read |= (0xff & bytes[offset + i]) << (i * 8);
		}
		return read;
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
		if (bytes == null)
			throw new IllegalArgumentException("Null byte array not allowed");
		
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
		if (byteCount < 0)
			throw new IllegalArgumentException("Cannot skip backward");
		
		checkOverflow(byteCount);
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
		checkOverflow(byteCount);
		byte[] readBytes = new byte[byteCount];
		System.arraycopy(bytes, offset, readBytes, 0, Math.min(byteCount, bytes.length - offset));
		offset += byteCount;
		return readBytes;
	}

	/**
	 * Reads a BoolState from this ByteArrayReader, consuming the indicated
	 * number of bytes.
	 */
	public BoolState readBoolState(int byteCount) {
		return new BoolState(readBytes(byteCount));
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
	public BitField readBitField(int bitCount) {
		checkOverflow(BitField.countBytes(bitCount));
		BitField bitField = new BitField(bitCount, bytes, offset);
		offset += bitField.getByteCount();
		return bitField;
	}

	/**
	 * Reads and returns a US ASCII encoded String().
	 */
	public String readUsAsciiString() {
		int byteCount = readInt();
		checkOverflow(byteCount);
		return Util.decode(readBytes(byteCount), Util.US_ASCII);
	}

	/**
	 * Reads and returns a UTF-16LE encoded String().
	 */
	public CharSequence readUtf16LeString() {
		int charCount = readInt();
		int byteCount = charCount + charCount;
		byte[] readBytes = readBytes(byteCount);
		return new NullTerminatedString(readBytes);
	}

	private void checkOverflow(int byteCount) {
		if (offset + byteCount > bytes.length) {
			throw new IndexOutOfBoundsException("Cannot move past end of byte stream");
		}
	}
}