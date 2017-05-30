package com.walkertribe.ian.protocol.core.fighter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.BaseArtemisPacket;
import com.walkertribe.ian.util.Version;

/**
 * Updates the current status of the fighter bays.
 * @author rjwut
 */
public class FighterBayStatusPacket extends BaseArtemisPacket implements Iterable<FighterBayStatusPacket.Bay> {
    private static final int TYPE = 0x9ad1f23b;

    public static class Bay {
    	private int id;
    	private int bayNum;
    	private String name;
    	private String className;
    	private int refitTime;

    	public Bay(int id, int bayNum, String name, String className, int refitTime) {
    		this.id = id;
    		this.bayNum = bayNum;
    		this.name = name;
    		this.className = className;
    		this.refitTime = refitTime;
    	}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
		
		public int getBayNumber() {
			return bayNum;
		}
		
		public void setBayNumber(int bayNum) {
			this.bayNum = bayNum;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getClassName() {
			return className;
		}

		public void setClassName(String className) {
			this.className = className;
		}

		public int getRefitTime() {
			return refitTime;
		}

		public void setRefitTime(int refitTime) {
			this.refitTime = refitTime;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (!(obj instanceof Bay)) {
				return false;
			}

			return id == ((Bay) obj).id;
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public String toString() {
			return String.format(Locale.getDefault(), "#%d (#%d): %s (%s): %d", id, bayNum, name, className, refitTime);
		}
    }

    public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.SERVER, TYPE, new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return FighterBayStatusPacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new FighterBayStatusPacket(reader);
			}
		});
	}

    private List<Bay> bays = new ArrayList<Bay>();

    private FighterBayStatusPacket(PacketReader reader) {
        super(ConnectionType.SERVER, TYPE);

        do {
        	int id = reader.readInt();

        	if (id == 0) {
        		break;
        	}

        	int bayNum = 0;
        	if (reader.getVersion().ge(new Version("2.6"))) bayNum = reader.readInt();
        	
        	String name = reader.readString();
        	String className = reader.readString();
        	int refitTime = reader.readInt();
        	bays.add(new Bay(id, bayNum, name, className, refitTime));
        } while (reader.hasMore());

        System.out.println(this);
    }

    public FighterBayStatusPacket() {
    	super(ConnectionType.SERVER, TYPE);
    }

	@Override
	public Iterator<FighterBayStatusPacket.Bay> iterator() {
		return bays.iterator();
	}

    @Override
	protected void writePayload(PacketWriter writer) {
    	for (Bay bay : bays) {
    		writer
    			.writeInt(bay.id)
    			.writeInt(bay.bayNum)
    			.writeString(bay.name)
    			.writeString(bay.className)
    			.writeInt(bay.refitTime);
    	}

    	writer.writeInt(0);
    }

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		for (Bay bay : bays) {
			b.append("\n\t").append(bay);
		}
	}
}
