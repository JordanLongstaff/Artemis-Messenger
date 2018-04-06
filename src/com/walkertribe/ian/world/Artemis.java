package com.walkertribe.ian.world;

/**
 * Contains miscellaneous constants about the game world.
 * @author rjwut
 */
public final class Artemis {
	/**
	 * The default port on which the Artemis server listens for connections.
	 */
	public static final String DEFAULT_PORT = "2010";

    /**
     * The maximum warp factor player ships can achieve.
     */
    public static final int MAX_WARP = 4;

    /**
     * The number of available player ships.
     */
    public static final int SHIP_COUNT = 8;

    /**
     * The length of the sides of the map (the X and Z dimensions).
     */
    public static final int MAP_SIZE = 100000;

    /**
     * No instantiation allowed.
     */
    private Artemis() { }
}