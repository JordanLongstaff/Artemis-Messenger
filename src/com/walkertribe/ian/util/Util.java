package com.walkertribe.ian.util;

import java.nio.charset.Charset;
import java.util.Set;

/**
 * Miscellaneous unloved stuff that doesn't have a home right now. But don't
 * worry, we love you and will find you a home.
 */
public class Util {
	public static final Charset US_ASCII = Charset.forName("US-ASCII");
	public static final Charset UTF16LE = Charset.forName("UTF-16LE");

	public static final String enumSetToString(Set<? extends Enum<?>> set) {
    	if (set.isEmpty()) {
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
}
