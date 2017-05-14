package com.walkertribe.ian.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Provides easy reading and writing of bits in a bit field. The bit places are
 * identified by an enum. The bytes are little-endian, so in the event that the
 * final byte is not completely utilized, it will be the most significant bits
 * that are left unused.
 * @author rjwut
 */
public class BitField {
	private byte[] bytes;

	/**
	 * Creates a BitField large enough to accommodate the enumerated bits. All
	 * bits start at 0.
	 */
	public BitField(Enum<?>[] bits) {
		this(bits.length);
	}

	/**
	 * Creates a BitField large enough to accommodate the enumerated bits, and
	 * stores the indicated bytes in it.
	 */
	public BitField(Enum<?>[] bits, byte[] bytes, int offset) {
		this.bytes = Arrays.copyOfRange(bytes, offset, offset + countBytes(bits));
	}

	/**
	 * Creates a BitField with the given number of bits. All bits start at 0.
	 */
	public BitField(int bitCount) {
		this.bytes = new byte[countBytes(bitCount)];
	}

	/**
	 * Returns the number of bytes in this BitField.
	 */
	public int getByteCount() {
		return bytes.length;
	}

	/**
	 * Returns true if the indicated bit is 1, false if it's 0.
	 */
	public boolean get(Enum<?> bit) {
		int bitIndex = bit.ordinal();
		int byteIndex =  bitIndex / 8;
		int mask = 0x1 << (bitIndex % 8);
		return (bytes[byteIndex] & mask) != 0;
	}

	/**
	 * If value is true, the indicated bit is set to 1; otherwise, it's set to
	 * 0.
	 */
	public void set(Enum<?> bit, boolean value) {
		int ordinal = bit.ordinal();
		int byteIndex = ordinal / 8;
		int bitIndex = ordinal % 8;
		int mask = (0x1 << bitIndex) ^ 0xff;
		int shiftedValue = (value ? 1 : 0) << bitIndex;
		bytes[byteIndex] = (byte) ((bytes[byteIndex] & mask) | shiftedValue);
	}

	/**
	 * Returns a hex encoded String of the bytes that comprise this BitField.
	 */
	@Override
	public String toString() {
		return TextUtil.byteArrayToHexString(bytes);
	}

	/**
	 * Writes this BitField to the given OutputStream.
	 */
	public void write(OutputStream out) throws IOException {
		out.write(bytes);
	}

	/**
	 * Returns a space-delimited list of the names of the enum values that
	 * correspond to active bits in this BitField. This can be useful for
	 * debugging purposes.
	 */
	public String listActiveBits(Enum<?>[] bits) {
		StringBuilder list = new StringBuilder();

		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];

			for (int j = 0; j < 8; j++) {
				int bitIndex = i * 8 + j;

				if (bitIndex < bits.length) {
					if ((b & (0x01 << j)) != 0) {
						if (list.length() != 0) {
							list.append(' ');
						}

						list.append(bits[bitIndex].name());
					}
				}
			}
		}

		return list.toString();
	}

	/**
	 * Returns the number of bytes required to store the given number of bits in a BitField.
	 */
	private static int countBytes(int bitCount) {
		return (bitCount + 7) / 8;
	}

	/**
	 * Returns the number of bytes required to store the enumerated bits. 
	 */
	private static int countBytes(Enum<?>[] bits) {
		return countBytes(bits.length);
	}
}