package com.walkertribe.ian.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Miscellaneous unloved stuff that doesn't have a home right now. But don't
 * worry, we love you and will find you a home.
 */
public final class Util {
	public static final Charset US_ASCII = Charset.forName("US-ASCII");
	public static final Charset UTF16LE = Charset.forName("UTF-16LE");
	public static final Charset UTF8 = Charset.forName("UTF-8");
	
	/**
	 * No instantiation allowed.
	 */
	private Util() { }
	
	/**
	 * Split the given String consisting of space-separated token into a Set of String tokens.
	 */
	public static Set<String> splitSpaceDelimited(CharSequence list) {
		Set<String> attrs = new LinkedHashSet<String>();
		for (String attr: list.toString().split(" ")) {
			if (attr.length() != 0) attrs.add(attr);
		}
		return attrs;
	}
	
	/**
	 * Reverses splitSpaceDelimited.
	 */
	public static String joinSpaceDelimited(Collection<String> strings) {
		StringBuilder b = new StringBuilder();
		for (String string: strings) b.append(' ').append(string);
		return b.substring(1).toString();
	}

	/**
	 * Returns a space-delimited list of the names of the enum values found in the given Set.
	 */
	public static String enumSetToString(Set<? extends Enum<?>> set) {
    	if (set == null || set.isEmpty()) {
    		return "";
    	}

    	StringBuilder b = new StringBuilder();

    	for (Enum<?> val : set) {
    		if (b.length() != 0) {
    			b.append(' ');
    		}

    		b.append(val);
    	}

    	return b.toString();
    }
	
	/**
	 * Returns true if the given collection contains any of the indicated objects;
	 * false otherwise.
	 */
	public static boolean containsAny(Collection<?> collection, Object... objs) {
		if (collection == null)
			throw new IllegalArgumentException("Null collection cannot be tested");
		
		for (Object obj: objs) {
			if (collection.contains(obj)) return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the given String contains any of the indicated CharSequences;
	 * false otherwise.
	 */
	public static boolean containsAny(String string, CharSequence... sequences) {
		if (string == null)
			throw new IllegalArgumentException("Null string cannot be tested");
		
		for (CharSequence seq: sequences) {
			if (string.contains(seq)) return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the given NullTerminatedString contains any of the indicated
	 * CharSequences; false otherwise.
	 */
	public static boolean containsAny(NullTerminatedString string, CharSequence... sequences) {
		if (string == null)
			throw new IllegalArgumentException("Null string cannot be tested");
		
		for (CharSequence seq: sequences) {
			if (string.contains(seq)) return true;
		}
		return false;
	}
	
	/**
	 * Returns true if the given CharSequence is null or zero-length.
	 */
	public static boolean isBlank(CharSequence str) {
		return str == null || str.length() == 0;
	}
	
	/**
	 * Converts carats (which Artemis uses for line breaks) to newline characters.
	 */
	public static CharSequence caratToNewline(CharSequence str) {
		return replace(str, '^', '\n');
	}
	
	/**
	 * Converts newline characters to carats (which Artemis uses for line breaks).
	 */
	public static CharSequence newlineToCarat(CharSequence str) {
		return replace(str, '\n', '^');
	}
	
	/**
	 * Replaces all instances of one character with another in the given CharSequence. This utility
	 * method is provided because replace() doesn't exist in CharSequence, and we need this in order
	 * to properly handle NullTerminatedStrings.
	 */
	private static CharSequence replace(CharSequence str, char oldChar, char newChar) {
		if (str instanceof String) return str.toString().replace(oldChar, newChar);
		if (str instanceof NullTerminatedString)
			return ((NullTerminatedString) str).replace(oldChar, newChar);
		
		// Somebody tossed a different kind of CharSequence our way, so do it manually
		int len = str.length();
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			b.append(c == oldChar ? newChar : c);
		}
		return b;
	}
	
	/**
	 * Returns a String decoded from the given byte array in the given Charset.
	 */
	public static String decode(byte[] bytes, Charset charset) {
		return charset.decode(ByteBuffer.wrap(bytes)).toString();
	}
}