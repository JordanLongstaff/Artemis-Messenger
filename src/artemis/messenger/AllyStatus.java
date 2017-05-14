package artemis.messenger;

/**
 * A list of statuses of ally ships, revealed when hailed.
 * @author Jordan Longstaff
 */
public enum AllyStatus {
	NORMAL("Normal"),
	REWARD("Delivering reward"),
	NEED_ENERGY("Need 100 energy"),
	FLYING_BLIND("Flying blind"),
	BROKEN_COMPUTER("Malfunction", "EMP to reset"),
	NEED_DAMCON("Need DamCon teams"),
	HOSTAGE("Hostages", "Demand 900 energy"),
	COMMANDEERED("Commandeered", "Approach in nebula"),
	FIGHTERS("Fighter trap", "Do not approach"),
	MINE_TRAP("Mine trap", "Do not approach");
	
	public final String m1, m2;
	AllyStatus(String m) {
		m1 = m;
		m2 = "";
	}
	AllyStatus(String m_1, String m_2) {
		m1 = m_1;
		m2 = m_2;
	}
}