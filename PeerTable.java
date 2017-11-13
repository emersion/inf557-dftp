import java.net.InetAddress;
import java.util.Date;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

class PeerTable {
	private static final Duration expiration = Duration.ofSeconds(10);

	enum State {HEARD, INCONSISTENT, SYNCHRONIZED, DYING}

	public static class Record {
		public final String id;
		public final InetAddress address;

		protected int lastSeqNum = 0;
		protected Instant expiresAt = null;
		protected State state = State.HEARD;
		protected Database db = new Database();

		protected Record(String id, InetAddress address) {
			this.id = id;
			this.address = address;
		}

		public int lastSeqNum() {
			return lastSeqNum;
		}

		public State state() {
			return state;
		}
	}

	private Map<String, Record> records = new HashMap<>();

	public synchronized List<Record> records() {
		cleanup();
		List<Record> list = new ArrayList<>();
		for (Record rec : records.values()) {
			list.add(rec);
		}
		return list;
	}

	public synchronized void update(String id, InetAddress address, int seqNum) {
		Record rec = records.get(id);
		if (rec == null) {
			rec = new Record(id, address);
			records.put(id, rec);
		} else {
			if (!address.equals(rec.address)) {
				throw new IllegalArgumentException("got two different IP addresses for the same peer ID");
			}
			if (rec.lastSeqNum != seqNum) {
				rec.state = State.INCONSISTENT;
			}
		}

		rec.expiresAt = Instant.now().plus(expiration);

		cleanup();
	}

	public synchronized void synchronize(String id, String[] data, int seqNum) {
		Record rec = records.get(id);
		if (rec == null) {
			throw new RuntimeException("attempt to synchronize a non-existing peer: "+id);
		}

		rec.db.update(data, seqNum);
		rec.lastSeqNum = seqNum;
	}

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
	}
}
