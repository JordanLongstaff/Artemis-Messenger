package com.walkertribe.ian.util;

import java.util.Arrays;

import com.walkertribe.ian.iface.PacketWriter;

/**
 * Version number handling class. This handles semantic versioning
 * (major.minor.patch), and can interpret float version numbers for backwards
 * compatibility. For robustness and to avoid duplication of code, it can handle
 * an arbitrary number of parts in the version number, not just three.
 * @author rjwut
 */
public class Version implements Comparable<Version> {
	private static final Version MODERN = new Version("2.1");

	private final int[] mParts;
	private final int hash;

	/**
	 * Constructs a Version from integer parts, with the most significant part
	 * first. This constructor can be used to create both modern and legacy
	 * version numbers.
	 */
	public Version(int... parts) {
		if (parts == null)
			throw new IllegalArgumentException("Version cannot be null");
		if (parts.length == 0)
			throw new IllegalArgumentException("Version must have at least one part");
		
		for (int part: parts) {
			if (part < 0)
				throw new IllegalArgumentException("Version numbers cannot be negative");
		}
		
		mParts = new int[Math.max(3, parts.length)];
		System.arraycopy(parts, 0, mParts, 0, Math.min(parts.length, mParts.length));
		hash = Arrays.hashCode(mParts);
	}

	/**
	 * Interprets a float as a version number, with a check to ensure that
	 * versions prior to 1.41 are interpreted correctly according to the Artemis
	 * version history. This constructor can only be used to create legacy
	 * version numbers (earlier than version 2.1); later ones will throw an
	 * IllegalArgumentException.
	 * @see http://artemiswiki.pbworks.com/w/page/53699717/Version%20history
	 */
	public Version(float version) {
		if (version < 0)
			throw new IllegalArgumentException("Version numbers cannot be negative");
		if (version > 2.0999999)
			throw new IllegalArgumentException("Legacy version constructor is not valid for Artemis 2.1+");

		int major = (int) Math.floor(version);
		int minor = (int) Math.floor((version - major) * 100);

		if (minor < 40) {
			minor /= 10;
		}

		mParts = new int[] { major, minor, 0 };
		hash = Arrays.hashCode(mParts);
	}

	/**
	 * Constructs a Version from a String. This constructor can be used to
	 * create both modern and legacy version numbers.
	 */
	public Version(String version) {
		if (version == null)
			throw new IllegalArgumentException("Version cannot be null");
		
		String[] strParts = version.split("\\.");
		mParts = new int[strParts.length];

		for (int i = 0; i < strParts.length; i++) {
			mParts[i] = Integer.parseInt(strParts[i].substring(0, 1));
			if (mParts[i] < 0)
				throw new IllegalArgumentException("Version numbers cannot be negative");
		}

		hash = Arrays.hashCode(mParts);
	}

	/**
	 * Returns true if this is a legacy version number; false otherwise. Legacy
	 * versioning was deprecated as of Artemis 2.1, so this method will return
	 * true for all version numbers earlier than that.
	 */
	public boolean isLegacy() {
		return lt(MODERN);
	}

	/**
	 * Convenience method for compareTo(version) < 0.
	 */
	public boolean lt(Version version) {
		return compareTo(version) < 0;
	}

	/**
	 * Convenience method for compareTo(version) > 0.
	 */
	public boolean gt(Version version) {
		return compareTo(version) > 0;
	}

	/**
	 * Convenience method for compareTo(version) <= 0.
	 */
	public boolean le(Version version) {
		return compareTo(version) <= 0;
	}

	/**
	 * Convenience method for compareTo(version) >= 0.
	 */
	public boolean ge(Version version) {
		return compareTo(version) >= 0;
	}

	/**
	 * Writes this Version to the given PacketWriter. Writes both the legacy and
	 * (if applicable) modern version fields. Note that to ensure compatibility,
	 * modern version fields will always be written with exactly three parts.
	 */
	public void writeTo(PacketWriter writer) {
		boolean legacy = isLegacy();
		writer.writeFloat(legacy ? mParts[0] + mParts[1] * 0.1f : 2.0f);

		if (!legacy) {
			for (int i = 0; i < 3; i++) {
				writer.writeInt(getPart(i));
			}
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof Version)) {
			return false;
		}

		return compareTo((Version) o) == 0;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public String toString() {
		if (isLegacy()) {
			return getPart(0) + "." + getPart(1);
		}

		StringBuilder b = new StringBuilder();

		for (int part: mParts) {
			if (b.length() != 0) {
				b.append('.');
			}

			b.append(part);
		}

		return b.toString();
	}

	/**
	 * Compares this Version against the given one. If the two Version objects
	 * don't have the same number of parts, the absent parts are treated as zero
	 * (e.g.: 2.1 is the same as 2.1.0).
	 */
	@Override
	public int compareTo(Version o) {
		int partCount = Math.max(mParts.length, o.mParts.length);

		for (int i = 0; i < partCount; i++) {
			int c = getPart(i) - o.getPart(i);

			if (c != 0) {
				return c;
			}
		}

		return 0;
	}

	/**
	 * Returns the indicated part value for the given array of parts, or 0 if
	 * the index is greater than that of the last part.
	 */
	private int getPart(int index) {
		return mParts.length > index ? mParts[index] : 0;
	}
}