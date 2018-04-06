package com.walkertribe.ian.protocol.core.world;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.world.ArtemisObject;

/**
 * <p>
 * A packet which contains updates for world objects.
 * </p>
 * <p>
 * While the ObjectUpdatePacket supports payloads with heterogeneous object type, in practice the
 * Artemis server only sends packets with homogeneous types; in other words, object updates of
 * different types are sent in separate packets. Initial tests seem to indicate that the stock
 * Artemis client can handle heterogeneous update types in a single packet, but this is not yet
 * considered 100% confirmed. If you wish to ensure that you completely emulate the behavior of an
 * Artemis server, send separate packets for separate object types. You can do this easily by
 * invoking segregate().
 * </p>
 * <p>
 * The ArtemisPlayer object is actually expressed in four different update types, depending on the
 * data that it contains:
 * </p>
 * <ul>
 * <li>
 *   <code>ObjectType.PLAYER</code>: Data not included in the other three types
 * </li>
 * <li>
 *   <code>ObjectType.WEAPONS_CONSOLE</code>: Data about ordnance counts and tube status 
 * </li>
 * <li>
 *   <code>ObjectType.ENGINEERING_CONSOLE</code>: Data about system status (energy, heat, coolant)
 * </li>
 * <li>
 *   <code>ObjectType.UPGRADES</code>: Data about upgrade status
 * </li>
 * </ul>
 * <p>
 * Under most circumstances, every object stored in the ObjectUpdatePacket object will produce one
 * update in the packet itself when it's written out. But ArtemisPlayer objects will produce one
 * update <strong>per update type</strong> listed above for each type that has any data in the
 * object. You can determine whether an ArtemisPlayer object contains data for a particular
 * ObjectType by calling ArtemisPlayer.hasDataForType(ObjectType). The
 * segregate() method takes this into consideration.
 * </p>
 *
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.OBJECT_BIT_STREAM)
public class ObjectUpdatePacket extends BaseArtemisPacket {
	private static final ObjectParser[] PARSERS = new ObjectParser[ObjectType.values().length];
	
	static {
		PARSERS[ObjectType.PLAYER_SHIP.ordinal()] = new PlayerShipParser();
		PARSERS[ObjectType.BASE.ordinal()] = new BaseParser();
		PARSERS[ObjectType.NPC_SHIP.ordinal()] = new NpcShipParser();
		
		for (ObjectType type: ObjectType.values()) {
			if (PARSERS[type.ordinal()] == null)
				PARSERS[type.ordinal()] = new UnobservedObjectParser(type);
		}
	}

	private List<ArtemisObject> objects = new LinkedList<ArtemisObject>();

	public ObjectUpdatePacket() { }

	public ObjectUpdatePacket(PacketReader reader) {
		do {
			ObjectType objectType = ObjectType.fromId(reader.peekByte());

			if (objectType == null) {
				break;
			}

			ObjectParser parser = PARSERS[objectType.ordinal()];
			ArtemisObject object = parser.parse(reader);
			if (object != null) objects.add(object);
		} while (true);

		reader.skip(4);
	}

	/**
	 * Add a new object to be updated.
	 */
	public void addObject(ArtemisObject obj) {
		objects.add(obj);
	}

	/**
	 * Add a Collection of objects to be updated.
	 */
	public void addObjects(Collection<ArtemisObject> objs) {
		objects.addAll(objs);
	}

	/**
	 * Returns the updated objects.
	 */
	public List<ArtemisObject> getObjects() {
		return new LinkedList<ArtemisObject>(objects);
	}

	@Override
	protected void writePayload(PacketWriter writer) { }

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		for (ArtemisObject obj: objects) b.append("\nObject #").append(obj.getId()).append(obj);
	}
}