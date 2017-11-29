import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A versioned database containing an array of strings.
 */
class Database {
	private String[] data;
	private int seqNum;

	/**
	 * Create a synchronized, populated database.
	 */
	public Database(String[] data, int seqNum) {
		this.data = data;
		this.seqNum = seqNum;
	}

	/**
	 * Updates the database. A database can only be updated with strictly
	 * increasing sequence numbers.
	 */
	public synchronized void update(String[] data, int seqNum) {
		if (this.seqNum >= seqNum) {
			throw new RuntimeException("attempt to update a Database with an older sequence number ("+seqNum+" < "+this.seqNum+")");
		}

		this.data = data;
		this.seqNum = seqNum;
	}

	/**
	 * Updates the database. The database's sequence number is automatically
	 * incremented.
	 */
	public synchronized void update(String[] data) {
		this.data = data;
		this.seqNum++;
	}

	/**
	 * Returns this database's data as a read-only list.
	 */
	public synchronized List<String> data() {
		return Collections.unmodifiableList(Arrays.asList(data));
	}

	public synchronized int seqNum() {
		return seqNum;
	}
}
