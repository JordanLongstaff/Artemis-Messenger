package com.walkertribe.ian.protocol.core.world;

import com.walkertribe.ian.enums.ConnectionType;
import com.walkertribe.ian.iface.PacketFactory;
import com.walkertribe.ian.iface.PacketFactoryRegistry;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.iface.PacketWriter;
import com.walkertribe.ian.protocol.ArtemisPacket;
import com.walkertribe.ian.protocol.ArtemisPacketException;
import com.walkertribe.ian.protocol.BaseArtemisPacket;

public class BeamFiredPacket extends BaseArtemisPacket {
	private static final int TYPE = 0xb83fd2c4;
	private static final byte[] DEFAULT_UNKNOWN_VALUE = { 0, 0, 0, 0 };

	public static void register(PacketFactoryRegistry registry) {
		registry.register(ConnectionType.SERVER, TYPE, new PacketFactory() {
			@Override
			public Class<? extends ArtemisPacket> getFactoryClass() {
				return BeamFiredPacket.class;
			}

			@Override
			public ArtemisPacket build(PacketReader reader)
					throws ArtemisPacketException {
				return new BeamFiredPacket(reader);
			}
		});
	}

	private int mBeamId;
	private int mBeamPortIndex;
	private int mOriginId;
	private int mTargetId;
	private float mImpactX;
	private float mImpactY;
	private float mImpactZ;
	private boolean mAutoFired;
	private byte[] unknown1 = DEFAULT_UNKNOWN_VALUE;
	private byte[] unknown2 = DEFAULT_UNKNOWN_VALUE;
	private byte[] unknown3 = DEFAULT_UNKNOWN_VALUE;
	private byte[] unknown4 = DEFAULT_UNKNOWN_VALUE;

	private BeamFiredPacket(PacketReader reader) {
		super(ConnectionType.SERVER, TYPE);
		mBeamId = reader.readInt();
		unknown1 = reader.readBytes(4);
		unknown2 = reader.readBytes(4);
		mBeamPortIndex = reader.readInt();
		unknown3 = reader.readBytes(4);
		unknown4 = reader.readBytes(4);
		mOriginId = reader.readInt();
		mTargetId = reader.readInt();
		mImpactX = reader.readFloat();
		mImpactY = reader.readFloat();
		mImpactZ = reader.readFloat();
		mAutoFired = reader.readInt() == 0;
	}

	public BeamFiredPacket(int beamId) {
		super(ConnectionType.SERVER, TYPE);
		mBeamId = beamId;
	}

	/**
	 * The beam's ID.
	 */
	public int getBeamId() {
		return mBeamId;
	}

	/**
	 * The index of the port from which this beam was fired, which is the same
	 * as the index of the corresponding <code>&lt;beam_port&gt;</code> entry in
	 * the shipData.xml file. (So 0 corresponds to the zeroeth
	 * <code>&lt;beam_port&gt;</code> entry for that ship in shipData.xml.)
	 */
	public int getBeamPortIndex() {
		return mBeamPortIndex;
	}

	public void setBeamPortIndex(int beamPortIndex) {
		mBeamPortIndex = beamPortIndex;
	}

	/**
	 * The ID of the object from which the beam was fired.
	 */
	public int getOriginId() {
		return mOriginId;
	}

	public void setOriginId(int originId) {
		mOriginId = originId;
	}

	/**
	 * The ID of the object being fired upon.
	 */
	public int getTargetId() {
		return mTargetId;
	}

	public void setTargetId(int targetId) {
		mTargetId = targetId;
	}

	/**
	 * The X-coordinate (relative to the center of the target) of the impact
	 * point. This is used to determine the endpoint for the beam. A negative
	 * value means an impact on the target's starboard; a positive value means
	 * an impact on the target's port.
	 */
	public float getImpactX() {
		return mImpactX;
	}

	public void setImpactX(float impactX) {
		mImpactX = impactX;
	}

	/**
	 * The Y-coordinate (relative to the center of the target) of the impact
	 * point. This is used to determine the endpoint for the beam. A negative
	 * value means an impact on the target's ventral (bottom) side; a positive
	 * value means an impact on the target's dorsal (top) side.
	 */
	public float getImpactY() {
		return mImpactY;
	}

	public void setImpactY(float impactY) {
		mImpactY = impactY;
	}

	/**
	 * The Z-coordinate (relative to the center of the target) of the impact
	 * point. This is used to determine the endpoint for the beam. A negative
	 * value means an impact on the target's aft; a positive value means an
	 * impact on the target's fore.
	 */
	public float getImpactZ() {
		return mImpactZ;
	}

	public void setImpactZ(float impactZ) {
		mImpactZ = impactZ;
	}

	/**
	 * Returns true if the beam was auto-fired; false if it was fired manually.
	 */
	public boolean isAutoFired() {
		return mAutoFired;
	}

	public void setAutoFired(boolean autoFired) {
		mAutoFired = autoFired;
	}

	@Override
	protected void writePayload(PacketWriter writer) {
		writer
			.writeInt(mBeamId)
			.writeBytes(unknown1)
			.writeBytes(unknown2)
			.writeInt(mBeamPortIndex)
			.writeBytes(unknown3)
			.writeBytes(unknown4)
			.writeInt(mOriginId)
			.writeInt(mTargetId)
			.writeFloat(mImpactX)
			.writeFloat(mImpactY)
			.writeFloat(mImpactZ)
			.writeInt(mAutoFired ? 0 : 1);
	}

	@Override
	protected void appendPacketDetail(StringBuilder b) {
		b
			.append("Beam #")
			.append(mBeamId)
			.append(" port ")
			.append(mBeamPortIndex)
			.append(" from ship #")
			.append(mOriginId)
			.append(" to ship #")
			.append(mTargetId);
	}
}