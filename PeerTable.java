import java.net.InetAddress;
import java.util.Date;
import java.time.Instant;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.ArrayList;

class PeerTable {
	private static final Duration expiration = Duration.ofSeconds(10);

	enum State {HEARD, INCONSISTENT, SYNCHRONIZED, DYING}

	public static class Record {
		public final String id;
		public final InetAddress address;

		protected int lastSeqNum = 0;
		protected Instant expiresAt = null;
		protected State state = State.HEARD; // TODO

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

	public Map<String, Record> records() {
		cleanup();
		return Collections.unmodifiableMap(records);
	}

	public void update(String id, InetAddress address, int seqNum) {
		Record rec = records.get(id);
		if (rec == null) {
			rec = new Record(id, address);
			records.put(id, rec);
		}

		if (!address.equals(rec.address)) {
			throw new IllegalArgumentException("got two different IP addresses for the same peer ID");
		}

		if (rec.lastSeqNum < seqNum) {
			rec.lastSeqNum = seqNum;
		}
		rec.expiresAt = Instant.now().plus(expiration);
		rec.state = State.HEARD;

		cleanup();
	}

	private void cleanup() {
		ArrayList<String> toRemove = new ArrayList<>();
		Instant now = Instant.now();
		for (Record rec : records.values()) {
			if (now.isAfter(rec.expiresAt)) {
				rec.state = State.DYING;
			}
		}
	}
}
