package com.walkertribe.ian.protocol.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.walkertribe.ian.enums.Origin;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.Packet;
import com.walkertribe.ian.util.Util;

/**
 * Describes why the game ended.
 * @author rjwut
 */
@Packet(origin = Origin.SERVER, type = CorePacketType.SIMPLE_EVENT, subtype = SimpleEventPacket.Subtype.GAME_REASON)
public class GameOverReasonPacket extends SimpleEventPacket {
	private final List<CharSequence> mText;

    public GameOverReasonPacket(CharSequence... text) {
    	if (text.length == 0)
    		throw new IllegalArgumentException("You must provide a reason why the game ended");
    	
    	for (CharSequence line: text) {
    		if (Util.isBlank(line))
    			throw new IllegalArgumentException("No blank lines allowed");
    	}
    	
    	mText = Arrays.asList(text);
    }
    
    public GameOverReasonPacket(PacketReader reader) {
    	super(reader);
    	mText = new LinkedList<CharSequence>();
    	while (reader.hasMore()) mText.add(reader.readString());
    }

    /**
     * The text describing why the game ended. Each CharSequence in the List is one line.
     */
    public List<CharSequence> getText() {
        return mText;
    }

	@Override
	protected void writePayload(PacketWriter writer) {
		super.writePayload(writer);
		for (CharSequence line: mText) writer.writeString(line);
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		for (CharSequence line: mText) b.append("\n\t").append(line);
	}
}