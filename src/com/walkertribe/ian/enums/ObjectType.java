package com.walkertribe.ian.enums;

import com.walkertribe.ian.world.ArtemisAnomaly;
import com.walkertribe.ian.world.ArtemisBase;
import com.walkertribe.ian.world.ArtemisCreature;
import com.walkertribe.ian.world.ArtemisDrone;
import com.walkertribe.ian.world.ArtemisGenericObject;
import com.walkertribe.ian.world.ArtemisMesh;
import com.walkertribe.ian.world.ArtemisNebula;
import com.walkertribe.ian.world.ArtemisNpc;
import com.walkertribe.ian.world.ArtemisObject;
import com.walkertribe.ian.world.ArtemisPlayer;

/**
 * World object types.
 * @author rjwut
 */
public enum ObjectType {
	PLAYER_SHIP(1, true, ArtemisPlayer.class),
	WEAPONS_CONSOLE(2, false, ArtemisPlayer.class),
	ENGINEERING_CONSOLE(3, false, ArtemisPlayer.class),
	UPGRADES(4, false, ArtemisPlayer.class),
	NPC_SHIP(5, true, ArtemisNpc.class),
	BASE(6, true, ArtemisBase.class),
	MINE(7, false, ArtemisGenericObject.class),
	ANOMALY(8, true, ArtemisAnomaly.class),
	// 9 is unused
	NEBULA(10, false, ArtemisNebula.class),
	TORPEDO(11, false, ArtemisGenericObject.class),
	BLACK_HOLE(12, false, ArtemisGenericObject.class),
	ASTEROID(13, false, ArtemisGenericObject.class),
	GENERIC_MESH(14, true, ArtemisMesh.class),
	CREATURE(15, true, ArtemisCreature.class),
	DRONE(16, false, ArtemisDrone.class);

	public static ObjectType fromId(int id) {
		if (id == 0) {
			return null;
		}

		for (ObjectType objectType : values()) {
			if (objectType.id == id) {
				return objectType;
			}
		}

		throw new IllegalArgumentException("No ObjectType with this ID: " + id);
	}

	private byte id;
	private boolean named;
	private Class<? extends ArtemisObject> objectClass;
	private float scale;

	ObjectType(int id, boolean named, Class<? extends ArtemisObject> objectClass) {
		this.id = (byte) id;
		this.named = named;
		this.objectClass = objectClass;
		scale = 0;
	}

	/**
	 * Returns the ID of this type.
	 */
	public byte getId() {
		return id;
	}

	/**
	 * Returns true if objects of this type can have a name; false otherwise.
	 */
	public boolean isNamed() {
		return named;
	}

	/**
	 * Returns true if the given object is compatible with this ObjectType.
	 */
	public boolean isCompatible(ArtemisObject obj) {
		return objectClass.equals(obj.getClass());
	}

	/**
	 * Returns the class of object represented by this ObjectType.
	 */
	public Class<? extends ArtemisObject> getObjectClass() {
		return objectClass;
	}

	/**
	 * Returns the base scale factor for this ObjectType's model, or 0.0 if this
	 * object has no model or has more than one possible model.
	 * @return
	 */
	public float getScale() {
		return scale;
	}
}