package com.walkertribe.ian.protocol.core.world;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.world.ArtemisObject;

/**
 * Abstract implementation of ObjectParser interface. Provides the common
 * object parsing behavior and delegates to the subclass's parseImpl() method
 * to read individual properties.
 * @author rjwut
 */
public abstract class AbstractObjectParser implements ObjectParser {
	protected abstract ArtemisObject parseImpl(PacketReader reader);

	protected ObjectType objectType;
	private Version version;

	protected AbstractObjectParser(ObjectType objectType) {
		this.objectType = objectType;
	}

	@Override
	public final ArtemisObject parse(PacketReader reader) {
		Version ver = reader.getVersion();
		if (version == null || !version.equals(ver)) {
			version = ver;
			reconcile(ver);
		}
		
		byte typeId = reader.hasMore() ? reader.readByte() : 0;

		if (typeId == 0) {
			return null; // no more objects to parse
		}

		reader.startObject(objectType, getBitCount());
		ArtemisObject obj = parseImpl(reader);
		if (obj != null) obj.setUnknownProps(reader.getUnknownObjectProps());
		return obj;
	}
}