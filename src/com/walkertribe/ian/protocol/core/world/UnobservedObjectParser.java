package com.walkertribe.ian.protocol.core.world;

import java.util.ArrayList;
import java.util.List;

import com.walkertribe.ian.enums.ObjectType;
import com.walkertribe.ian.iface.PacketReader;
import com.walkertribe.ian.util.Version;
import com.walkertribe.ian.world.ArtemisObject;

public class UnobservedObjectParser extends AbstractObjectParser {
	private enum BitCount {
		PLAYER_SHIP {
			@Override
			int[] getByteCounts() { return new int[0]; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		WEAPONS_CONSOLE {
			private final int minTime = 8, maxTime = 14;
			
			@Override
			int[] getByteCounts() {
				int[] counts = new int[25];
				for (int i = 0; i < counts.length; i++) counts[i] = 1;
				for (int i = minTime; i < maxTime; i++) counts[i] = 4;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		ENGINEERING_CONSOLE {
			private final int numFloats = 16;
			
			@Override
			int[] getByteCounts() {
				int[] counts = new int[24];
				for (int i = 0; i < numFloats; i++) counts[i] = 4;
				for (int i = numFloats; i < counts.length; i++) counts[i] = 1;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		UPGRADES {
			private final int numEights = 56;
			
			@Override
			int[] getByteCounts() {
				int[] counts = new int[84];
				for (int i = 0; i < numEights; i++) counts[i] = 1;
				for (int i = numEights; i < counts.length; i++) counts[i] = 2;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		NPC_SHIP {
			@Override
			int[] getByteCounts() { return new int[0]; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		BASE {
			@Override
			int[] getByteCounts() { return new int[0]; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		MINE {
			@Override
			int[] getByteCounts() { return new int[] { 4, 4, 4 }; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		ANOMALY {
			@Override
			int[] getByteCounts() { return new int[] { 1, 1, 1, 4, 4, 4, 1, 1 }; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) {
				refresh(byteCounts);
				
				if (version.lt(BEACON_VERSION)) {
					byteCounts.remove(6);
					byteCounts.remove(6);
					
					if (version.lt(FIGHTER_VERSION)) {
						byteCounts.remove(4);
						byteCounts.remove(4);
					}
				}
			}
		},
		NEBULA {
			@Override
			int[] getByteCounts() {
				int[] counts = new int[7];
				for (int i = 0; i < counts.length; i++) counts[i] = 4;
				counts[6] = 1;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		TORPEDO {
			@Override
			int[] getByteCounts() {
				int[] counts = new int[8];
				for (int i = 0; i < counts.length; i++) counts[i] = 4;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		BLACK_HOLE {
			@Override
			int[] getByteCounts() { return new int[] { 4, 4, 4 }; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		ASTEROID {
			@Override
			int[] getByteCounts() { return new int[] { 4, 4, 4 }; }
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		},
		GENERIC_MESH {
			@Override
			int[] getByteCounts() {
				return new int[] {
						4, 4, 4, 4, 4, 4, 4, 4,
						4, 4, 4, 4, S, S, S, 4,
						1, 4, 4, 4, 4, 4, 4, 1,
						S, S, 4
				};
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) {
				refresh(byteCounts);
				
				if (version.lt(CURRENT_VERSION))
					byteCounts.remove(26);
			}
		},
		CREATURE {
			@Override
			int[] getByteCounts() {
				int[] counts = new int[18];
				for (int i = 0; i < counts.length; i++) counts[i] = 4;
				counts[3] = S;
				counts[16] = 1;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) {
				refresh(byteCounts);
				
				if (version.lt(CURRENT_VERSION)) {
					byteCounts.remove(17);
					
					if (version.lt(BEACON_VERSION)) {
						byteCounts.remove(16);
						
						if (version.lt(FIGHTER_VERSION)) {
							byteCounts.remove(14);
							byteCounts.remove(14);
						}
					}
				}
			}
		},
		DRONE {
			@Override
			int[] getByteCounts() {
				int[] counts = new int[9];
				for (int i = 0; i < counts.length; i++) counts[i] = 4;
				return counts;
			}
			
			@Override
			void reconcile(List<Integer> byteCounts, Version version) { }
		};
		
		private static final Version FIGHTER_VERSION = new Version("2.3.0");
		private static final Version BEACON_VERSION = new Version("2.6.3");
		private static final Version CURRENT_VERSION = new Version("2.7.0");
		
		abstract int[] getByteCounts();
		abstract void reconcile(List<Integer> byteCounts, Version version);
		void refresh(List<Integer> byteCounts) {
			int[] stdCounts = getByteCounts();
			for (int i = 0; i < byteCounts.size(); i++)
				byteCounts.set(i, stdCounts[i]);
			for (int i = byteCounts.size(); i < stdCounts.length; i++)
				byteCounts.add(stdCounts[i]);
		}
	}
	
	private static final int S = -1;
	
	private final ArrayList<Integer> byteCounts;
	private final BitCount countIndex;
	
	protected UnobservedObjectParser(ObjectType objectType) {
		super(objectType);
		countIndex = BitCount.values()[objectType.ordinal()];
		int[] bytes = countIndex.getByteCounts();
		byteCounts = new ArrayList<Integer>(bytes.length);
		for (int b: bytes) byteCounts.add(b);
	}

	@Override
	public int getBitCount() {
		return byteCounts.size();
	}

	@Override
	protected ArtemisObject parseImpl(PacketReader reader) {
		for (int i = 0; i < byteCounts.size(); i++) {
			int byteCount = byteCounts.get(i);
			if (byteCount == S) reader.readString(i);
			else reader.readBytes(i, byteCount);
		}
		return null;
	}
	
	@Override
	public void reconcile(Version version) {
		countIndex.reconcile(byteCounts, version);
	}
}