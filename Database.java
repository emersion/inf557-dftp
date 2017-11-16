import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
	 * Update the database. A database can only be updated with strictly
	 * increasing sequence numbers.
	 */
	public synchronized void update(String[] data, int seqNum) {
		if (this.seqNum >= seqNum) {
			throw new RuntimeException("attempt to update a Database with an older sequence number ("+seqNum+" < "+this.seqNum+")");
		}

		this.data = data;
		this.seqNum = seqNum;
	}

	public synchronized void update(String[] data) {
		this.data = data;
		this.seqNum++;
	}

	public synchronized List<String> data() {
		return Collections.unmodifiableList(Arrays.asList(data));
	}

	public synchronized int seqNum() {
		return seqNum;
	}
}
