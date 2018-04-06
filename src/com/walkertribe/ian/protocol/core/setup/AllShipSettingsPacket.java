package com.walkertribe.ian.protocol.core.setup;

import com.walkertribe.ian.ArtemisContext;
import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.protocol.core.SimpleEventPacket;
import com.walkertribe.ian.util.Util;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.vesseldata.Vessel;
import com.walkertribe.ian.world.Artemis;

/**
 * Sent by the server to update the names, types and drives for each ship.
 * @author dhleong
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.SIMPLE_EVENT, subtype = SimpleEventPacket.Subtype.SHIP_SETTINGS)
public class AllShipSettingsPacket extends SimpleEventPacket {
	public static class Ship {
		private CharSequence mName;
		private int mShipType;
		
		public Ship(CharSequence name, int shipType) {
			setName(name);
			setShipType(shipType);
		}

		/**
		 * The name of the ship
		 */
		public CharSequence getName() {
			return mName;
		}

		public void setName(CharSequence name) {
			if (!Util.isBlank(name))
				mName = name;
		}

		/**
		 * The hullId for this ship
		 */
		public int getShipType() {
			return mShipType;
		}

		public void setShipType(int shipType) {
			mShipType = shipType;
		}

		/**
		 * Returns the Vessel identified by the ship's hull ID, or null if no such Vessel can be found.
		 */
		public Vessel getVessel(ArtemisContext ctx) {
			return ctx.getVesselData().getVessel(mShipType);
		}

		public void setVessel(Vessel vessel) {
			setShipType(vessel.getId());
		}

		@Override
		public String toString() {
			return mName + ": (type #" + mShipType + ")";
		}
	}
	
	private static final Version COLOR_VERSION = new Version("2.4.0");

	private final Ship[] mShips;

    public AllShipSettingsPacket(PacketReader reader) {
        super(reader);
        mShips = new Ship[Artemis.SHIP_COUNT];
        
        for (int i = 0; i < Artemis.SHIP_COUNT; i++) {
        	reader.readInt();
        	int hullId = reader.readInt();
        	if (reader.getVersion().ge(COLOR_VERSION)) reader.readFloat();
        	CharSequence name = null;
        	if (reader.readInt() != 0) name = reader.readString();
        	mShips[i] = new Ship(name, hullId);
        }
    }

    public AllShipSettingsPacket(Ship[] ships) {
        super(Subtype.SHIP_SETTINGS);
        
        if (ships == null)
        	throw new IllegalArgumentException("Ship array cannot be null");
        if (ships.length != Artemis.SHIP_COUNT)
        	throw new IllegalArgumentException("Must specify exactly " + Artemis.SHIP_COUNT + " ships");
        for (int i = 0; i < ships.length; i++) {
        	Ship ship = ships[i];
        	if (ship == null)
            	throw new IllegalArgumentException("Ships in array cannot be null");
        }

        mShips = ships;
    }

    /**
     * Returns the ship with the given index (0-based).
     */
    public Ship getShip(int shipIndex) {
    	return mShips[shipIndex];
    }

	@Override
	protected void writePayload(PacketWriter writer) {
		super.writePayload(writer);

		for (Ship ship: mShips) {
			writer
			.writeInt(0)
			.writeInt(ship.mShipType);
			
			if (writer.getVersion().ge(COLOR_VERSION))
				writer.writeFloat(0f);
			
			if (ship.mName == null) writer.writeInt(0);
			else                    writer.writeInt(1).writeString(ship.mName);
		}
	}

    @Override
	protected void appendPacketDetail(StringBuilder b) {
        for (int i = 0; i < Artemis.SHIP_COUNT; i++)
        	b.append("\n\t").append(mShips[i]);
	}
}