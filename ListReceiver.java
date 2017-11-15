import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Map;
import java.util.HashMap;

class ListReceiver implements MessageHandler, Runnable {
	private PeerTable peerTable;
	private String local;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);
	private Map<String, PendingReception> pending = new HashMap<>();

	/**
	 * A pending reception is an in-progress LIST message reception. It collects
	 * all LIST messages sent by a specific peer until a complete database is
	 * received.
	 */
	private static class PendingReception {
		public final String peer;
		public final int seqNum;
		public final int total;

		private int received = 0;
		private String[] data;

		public PendingReception(String peer, int seqNum, int total) {
			this.peer = peer;
			this.seqNum = seqNum;
			this.total = total;

			this.data = new String[total];
		}

		/**
		 * Checks if the received database is complete.
		 */
		public boolean done() {
			return received == total;
		}

		/**
		 * Receives a part of the database.
		 */
		public void receive(int partNum, String row) {
			if (data[partNum] != null) {
				// Rows, once set, are immutable
				if (!data[partNum].equals(row)) {
					System.err.println("received two different values for row "+partNum+" from peer "+peer);
				}
				return;
			}
			data[partNum] = row;
			++received;
		}

		/**
		 * Returns the complete database contents. Can only be used once the
		 * received database is complete.
		 */
		public String[] data() {
			if (!done()) {
				throw new RuntimeException("Attempt to retrieve a incomplete database");
			}
			return data;
		}
	}

	public ListReceiver(PeerTable peerTable, String local) {
		this.peerTable = peerTable;
		this.local = local;
	}

	public void handleMessage(Envelope env) {
		if (!(env.msg instanceof Message.List)) {
			return;
		}

		incoming.offer(env);
	}

	public void run() {
		while (true) {
			Envelope env;
			try {
				env = incoming.take();
			} catch (InterruptedException e) {
				break;
			}

			Message.List list = (Message.List)env.msg;
			if (!local.equals(list.peer)) {
				continue; // Not for me
			}

			try {
				peerTable.update(list.sender, env.address, list.seqNum);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			PeerTable.Record rec = peerTable.get(list.sender);
			if (rec.state() == PeerTable.State.SYNCHRONIZED) {
				// Already synchronized, we don't need this LIST message
				continue;
			}
			if (list.seqNum < rec.pendingSeqNum()) {
				// We need a newer version of the peer database
				continue;
			}

			// Check if there's already a pending reception for this peer
			// The LIST sequence number may be newer than the pending reception, in
			// this case overwrite the old pending reception
			PendingReception pr = pending.get(list.sender);
			if (pr == null || pr.seqNum < list.seqNum) {
				pr = new PendingReception(list.sender, list.seqNum, list.totalParts);
				pending.put(list.sender, pr);
			} else {
				if (pr.seqNum > list.seqNum) {
					// Received a LIST message for an old SYN
					continue;
				}
				if (pr.total != list.totalParts) {
					// The LIST total cannot change
					System.err.println("Received a LIST with total "+list.totalParts+", which is different from previous ones (expected "+pr.total+")");
					pending.remove(list.sender);
					continue;
				}
			}

			// partNum is already checked by Message.List
			pr.receive(list.partNum, list.data);

			if (pr.done()) {
				// We got a complete database, synchronize the peer
				try {
					peerTable.synchronize(pr.peer, pr.data(), pr.seqNum);
				} catch (Exception e) {
					System.out.println("Cannot synchronize peer "+pr.peer);
					e.printStackTrace();
					continue;
				}
				pending.remove(list.sender);
			}
		}
	}
}
