package com.walkertribe.ian.util;

import java.util.LinkedList;
import java.util.List;

/**
 * Stores Artemis's UTF-16 null-terminated strings, preserving any "garbage" data that may follow
 * the null. This ensures that string reading and writing is symmetrical; in other words, that the
 * bytes written always exactly match the bytes read. 
 * @author rjwut
 */
public class NullTerminatedString implements CharSequence {
	/**
	 * Converts a List of NullTerminatedStrings to a List of Strings.
	 */
	public static List<String> toStrings(List<NullTerminatedString> ntsList) {
		List<String> strings = new LinkedList<String>();
		
		for (NullTerminatedString nts: ntsList) {
			strings.add(nts != null ? nts.toString() : null);
		}
		
		return strings;
	}
	
	/**
	 * Converts a List of Strings to a List of NullTerminatedStrings.
	 */
	public static List<NullTerminatedString> toNTStrings(List<String> strings) {
		List<NullTerminatedString> ntsList = new LinkedList<NullTerminatedString>();
		
		for (String str: strings) {
			ntsList.add(str != null ? new NullTerminatedString(str) : null);
		}
		
		return ntsList;
	}
	
	private final String str;
	private final byte[] garbage;
	
	/**
	 * Reads a string from the given byte array.
	 */
	public NullTerminatedString(byte[] bytes) {
		 int i;
		 
		 // find the null
		 for (i = 0; i < bytes.length; i += 2) {
			 if (bytes[i] == 0 && bytes[i + 1] == 0) break;
		 }
		 
		 if (i == bytes.length)
			 throw new IllegalArgumentException("No null found for null-terminated string");
		 
		 byte[] string = new byte[i];
		 System.arraycopy(bytes, 0, string, 0, i);
		 str = Util.decode(string, Util.UTF16LE);
		 
		 i += 2;
		 garbage = new byte[bytes.length - i];
		 System.arraycopy(bytes, i, garbage, 0, garbage.length);
	}
	
	/**
	 * Converts a regular String into a NullTerminatedString.
	 */
	public NullTerminatedString(String string) {
		this(string, new byte[0]);
	}
	
	private NullTerminatedString(String string, byte[] bytes) {
		if (string == null || string.length() == 0)
			throw new IllegalArgumentException("NullTerminatedString must have at least one character");
		
		str = string;
		garbage = bytes;
	}
	
	/**
	 * Returns the "garbage" bytes. Returns an empty array if there were no garbage bytes.
	 */
	public byte[] getGarbage() {
		return garbage;
	}
	
	@Override
	public String toString() {
		return str;
	}

	@Override
	public char charAt(int index) {
		return str.charAt(index);
	}

	/**
	 * Returns the length of this NullTerminatedString in characters, excluding the null and any
	 * garbage data.
	 */
	@Override
	public int length() {
		return str.length();
	}
	
	/**
	 * Returns the length of this string in double-byte characters, including the null and any
	 * garbage data.
	 */
	public int fullLength() {
		return str.length() + garbage.length / 2 + 1;
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return str.subSequence(start, end);
	}
	
	/**
	 * Returns true if this NullTerminatedString contains the given CharSequence; false
	 * otherwise.
	 */
	public boolean contains(CharSequence other) {
		return str.contains(other);
	}
	
	/**
	 * Returns a new NullTerminatedString with all instances of oldChar replaced with newChar
	 * and with any garbage data preserved.
	 */
	public CharSequence replace(char oldChar, char newChar) {
		byte[] newGarbage = new byte[garbage.length];
		System.arraycopy(garbage, 0, newGarbage, 0, garbage.length);
		return new NullTerminatedString(str.replace(oldChar, newChar), newGarbage);
	}
}