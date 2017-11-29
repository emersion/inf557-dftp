import java.net.InetAddress;
import java.util.Date;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * A peer table holds information about all known peers.
 *
 * It notifies when a peer is added or removed and when a peer's state changes.
 */
class PeerTable {
	private static final Duration expiration = Duration.ofSeconds(10);
	private static final Duration minSynInterval = Duration.ofSeconds(1);

	enum State {HEARD, INCONSISTENT, SYNCHRONIZED, DYING}

	public static class Record {
		public final String id;
		public final InetAddress address;

		protected int pendingSeqNum = Integer.MIN_VALUE;
		protected Instant expiresAt = null;
		protected Instant nextSynAt = null;
		protected State state = State.HEARD;
		protected Database db = null;

		protected Record(String id, InetAddress address) {
			this.id = id;
			this.address = address;
		}

		/**
		 * Returns the sequence number of this peer's synchronized database.
		 */
		public synchronized int seqNum() {
			if (db == null) {
				return Integer.MIN_VALUE;
			}
			return db.seqNum();
		}

		/**
		 * Returns the last received (maybe not yet synchronized) sequence number.
		 */
		public synchronized int pendingSeqNum() {
			return pendingSeqNum;
		}

		public synchronized State state() {
			return state;
		}

		/**
		 * Returns the synchronized database of a peer. If the database hasn't been
		 * synchronized yet, returns null.
		 */
		public synchronized Database database() {
			return db;
		}

		/**
		 * Checks whether a synchronize request needs to be sent to this peer.
		 * This method ensures that the peer is in a state needing synchronization
		 * and that a synchronization request hasn't been sent for a while.
		 */
		public synchronized boolean requestSynchronize() {
			if (state != State.HEARD && state != State.INCONSISTENT) {
				return false;
			}
			Instant now = Instant.now();
			if (nextSynAt != null && !now.isAfter(nextSynAt)) {
				return false;
			}
			nextSynAt = now.plus(minSynInterval);
			return true;
		}
	}

	private Map<String, Record> records = new HashMap<>();

	/**
	 * Returns a read-only list of all peer records in the peer table.
	 */
	public synchronized List<Record> records() {
		cleanup();
		List<Record> list = new ArrayList<>();
		for (Record rec : records.values()) {
			list.add(rec);
		}
		return Collections.unmodifiableList(list);
	}

	/**
	 * Returns a single peer record from the peer table. Returns null if the peer
	 * is unknown.
	 */
	public synchronized Record get(String id) {
		return records.get(id);
	}

	/**
	 * Updates a peer's sequence number in the peer table. This method fails with
	 * an exception if the address is incorrect (meaning the sender is spoofing
	 * someone else's peer ID).
	 */
	public synchronized void update(String id, InetAddress address, int seqNum) {
		Record rec = records.get(id);
		if (rec == null) {
			rec = new Record(id, address);
			records.put(id, rec);
		} else {
			if (!address.equals(rec.address)) {
				throw new IllegalArgumentException("got two different IP addresses for the same peer ID");
			}
		}

		synchronized (rec) {
			rec.expiresAt = Instant.now().plus(expiration);
			rec.pendingSeqNum = seqNum;

			if (rec.seqNum() < seqNum && rec.state != State.DYING) {
				rec.state = State.INCONSISTENT;
			}
		}

		cleanup();
		this.notify();
	}

	/**
	 * Marks a peer as dying. This method fails with an exception if the address
	 * is incorrect.
	 */
	public synchronized void die(String id, InetAddress address) {
		Record rec = records.get(id);
		if (rec == null) {
			return;
		}

		if (!address.equals(rec.address)) {
			throw new IllegalArgumentException("got two different IP addresses for the same peer ID");
		}

		synchronized (rec) {
			rec.state = State.DYING;
		}

		cleanup();
		this.notify();
	}

	/**
	 * Synchronizes a peer's database.
	 */
	public synchronized void synchronize(String id, String[] data, int seqNum) {
		Record rec = records.get(id);
		if (rec == null) {
			throw new RuntimeException("attempt to synchronize a non-existing peer: "+id);
		}

		synchronized (rec) {
			if (rec.db == null) {
				rec.db = new Database(data, seqNum);
			} else {
				rec.db.update(data, seqNum);
			}
			if (rec.state != State.DYING) {
				rec.state = State.SYNCHRONIZED;
			}
		}

		cleanup();
		this.notify();
	}

	/**
	 * Prunes expired peers in the peer table. Notifies if the peer table has
	 * changed.
	 */
	private synchronized void cleanup() {
		ArrayList<String> toRemove = new ArrayList<>();
		Instant now = Instant.now();
		for (Record rec : records.values()) {
			if (now.isAfter(rec.expiresAt)) {
				toRemove.add(rec.id);
			}
		}
		for (String id : toRemove) {
			records.remove(id);
		}
		if (toRemove.size() > 0) {
			this.notify();
		}
	}
}
