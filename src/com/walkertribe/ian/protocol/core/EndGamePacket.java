package com.walkertribe.ian.protocol.core;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.protocol.Packet;

/**
 * Sent by the server when the game ends.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.SIMPLE_EVENT, subtype = SimpleEventPacket.Subtype.END_GAME)
public class EndGamePacket extends SimpleEventPacket {
    public EndGamePacket() { }
    
    public EndGamePacket(PacketReader reader) {
    	super(reader);
    }
    
    @Override
    protected void appendPacketDetail(StringBuilder b) { }
}