package com.walkertribe.ian.protocol;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.TextUtil;

/**
 * An abstract Protocol implementation which provides a method to register
 * PacketFactories by providing an array of ArtemisPacket subclasses to
 * register. It assumes a convention that each packet class has a public void
 * static method named "register" that accepts a PacketFactoryRegistry object,
 * which the method will use to register one or more PacketFactory instances to
 * handle that class.
 * @author rjwut
 */
public abstract class AbstractProtocol implements Protocol {
	Map<Key, Factory<?>> registry = new HashMap<Key, Factory<?>>();
	
	/**
	 * Invoked by the Protocol implementation to register a single packet
	 * class. The class must have a Packet annotation and an accessible
	 * constructor with a single PacketReader argument.
	 */
	protected <T extends ArtemisPacket> void register(Class<T> clazz) {
		Factory<?> factory;

		try {
			factory = new Factory<T>(clazz);
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex); // shouldn't happen
		}

		Packet anno = clazz.getAnnotation(Packet.class);

		if (anno == null) {
			throw new IllegalArgumentException(clazz + " has no @Packet annotation");
		}

		int type = BaseArtemisPacket.getHash(anno);
		byte[] subtypes = anno.subtype();

		if (subtypes.length == 0)
			registry.put(new Key(type, null), factory);
		else for (byte subtype: subtypes)
			registry.put(new Key(type, subtype), factory);
	}

	@Override
	public PacketFactory<?> getFactory(int type, Byte subtype) {
		PacketFactory<?> factory = registry.get(new Key(type, subtype));

		if (factory == null && subtype != null) {
			// no factory found for that subtype; try without subtype
			factory = registry.get(new Key(type, null));
		}

		return factory;
	}

	/**
	 * Entries in the registry are stored in a Map using this class as the key.
	 * @author rjwut
	 */
	private class Key {
		private final int type;
		private final Byte subtype;
		private final int hashCode;

		/**
		 * Creates a new Key for this type and subtype.
		 */
		private Key(int type, Byte subtype) {
			this.type = type;
			this.subtype = subtype;
			hashCode = Arrays.hashCode(new Object[] { Integer.valueOf(type), subtype });
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Key)) {
				return false;
			}

			Key that = (Key) obj;
			if (type != that.type) return false;
			if (subtype == null && that.subtype == null) return true;
			if (subtype == null || that.subtype == null) return false;
			return subtype.equals(that.subtype);
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public String toString() {
			return TextUtil.intToHexLE(type) + ":" +
					(subtype != null ? TextUtil.byteToHex(subtype.byteValue()) : "--");
		}
	}

	/**
	 * PacketFactory implementation that invokes a constructor with a
	 * PacketReader argument.
	 * @author rjwut
	 */
	private class Factory<T extends ArtemisPacket> implements PacketFactory<T> {
		private Class<T> clazz;
		private Constructor<T> constructor;

		private Factory(Class<T> clazz) throws ReflectiveOperationException {
			this.clazz = clazz;
			constructor = clazz.getDeclaredConstructor(PacketReader.class);
		}

		@Override
		public Class<T> getFactoryClass() {
			return clazz;
		}

		public T build(PacketReader reader) throws ArtemisPacketException {
			try {
				return constructor.newInstance(reader);
			} catch (InvocationTargetException ex) {
				throw new ArtemisPacketException(ex.getCause());
			} catch (ReflectiveOperationException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}