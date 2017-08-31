package artemis.messenger;

/**
 * A list of statuses of ally ships, revealed when hailed.
 * @author Jordan Longstaff
 */
public enum AllyStatus {
	NORMAL(6, "Normal"),
	REWARD(6, "Delivering reward"),
	NEED_ENERGY(2, "Need 100 energy"),
	FLYING_BLIND(5, "Flying blind"),
	BROKEN_COMPUTER(4, "Malfunction", "EMP to reset"),
	NEED_DAMCON(3, "Need DamCon teams"),
	HOSTAGE(0, "Hostages", "Demand 900 energy"),
	COMMANDEERED(1, "Commandeered", "Approach in nebula"),
	FIGHTERS(7, "Fighter trap", "Do not approach"),
	MINE_TRAP(7, "Mine trap", "Do not approach");
	
	public final String m1, m2;
	public final int index;
	AllyStatus(int i, String m) {
		index = i;
		m1 = m;
		m2 = "";
	}
	AllyStatus(int i, String m_1, String m_2) {
		index = i;
		m1 = m_1;
		m2 = m_2;
	}
}