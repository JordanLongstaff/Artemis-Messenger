package com.walkertribe.ian.protocol.core.setup;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.protocol.core.CorePacketType;
import com.walkertribe.ian.protocol.core.ValueIntPacket;
import com.walkertribe.ian.world.Artemis;

/**
 * Set the ship you want to be on. You must send this packet before
 * SetConsolePacket.
 * @author dhleong
 */
@Packet(origin = Origin.CLIENT, type = CorePacketType.VALUE_INT, subtype = ValueIntPacket.Subtype.SET_SHIP)
public class SetShipPacket extends ValueIntPacket {
	/**
     * Selects a ship to use during setup. Note that Artemis ship numbers are
     * one-based.
     */
    public SetShipPacket(int shipIndex) {
    	super(shipIndex);
    	
    	if (shipIndex < 0 || shipIndex >= Artemis.SHIP_COUNT)
    		throw new IllegalArgumentException("Ship index must be in the range [0," + Artemis.SHIP_COUNT + ")");
    }

    private SetShipPacket(PacketReader reader) {
        super(reader);
    }
    
    /**
     * The ship index being selected (0-based).
     */
    public int getShipIndex() {
    	return mArg;
    }

    @Override
	protected void appendPacketDetail(StringBuilder b) {
		b.append(mArg + 1);
	}
}