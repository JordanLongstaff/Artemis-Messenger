package artemis.messenger;

/**
 * A list of statuses of ally ships, revealed when hailed.
 * @author Jordan Longstaff
 */
public enum AllyStatus {
	NORMAL(9, "Normal"),
	REWARD(9, "Delivering reward"),
	NEED_ENERGY(2, "Need 100 energy"),
	FLYING_BLIND(5, "Flying blind"),
	BROKEN_COMPUTER(4, "Malfunction", "EMP to reset"),
	NEED_DAMCON(3, "Need DamCon teams"),
	AMBASSADOR(6, "Ambassador", "Rescue w/shuttle"),
	PIRATE_SUPPLIES(7, "Has Pirate contraband", "Intercept w/warship", "Keep warships away"),
	PIRATE_DATA(8, "Has secure data", "Keep enemies away", "Do not approach"),
	HOSTAGE(0, "Hostages", "Demand 900 energy"),
	COMMANDEERED(1, "Commandeered", "Approach in nebula"),
	FIGHTERS(10, "Fighter trap", "Do not approach"),
	MINE_TRAP(10, "Mine trap", "Do not approach"),
	DESTROYED(11, "Destroyed");
	
	final String m1, m2, m3;
	final int index;
	AllyStatus(int i, String m) {
		index = i;
		m1 = m;
		m2 = "";
		m3 = "";
	}
	AllyStatus(int i, String m_1, String m_2) {
		index = i;
		m1 = m_1;
		m2 = m_2;
		m3 = "";
	}
	AllyStatus(int i, String m_1, String m_2, String m_3) {
		index = i;
		m1 = m_1;
		m2 = m_2;
		m3 = m_3;
	}
}