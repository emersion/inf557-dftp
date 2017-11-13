import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.Map;
import java.util.HashMap;

class ListReceiver implements MessageHandler, Runnable {
	private PeerTable peerTable;
	private String local;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);
	private Map<String, PendingReception> pending = new HashMap<>();

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

		public boolean done() {
			return received == total;
		}

		public void receive(int partIndex, String row) {
			data[partIndex] = row;
		}

		public String[] data() {
			if (!done()) {
				throw new RuntimeException("Attempt to retrieve a incomplete database");
			}
			return data;
		}
	}

	public ListReceiver(PeerTable peerTable, String local) {
		this.peerTable = peerTable;
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

			PendingReception pr = pending.get(list.sender);
			if (pr == null || pr.seqNum < list.seqNum) {
				pr = new PendingReception(list.sender, list.seqNum, list.totalParts);
			} else {
				if (pr.seqNum > list.seqNum) {
					// Received LIST message for an old SYN
					continue;
				}
				if (pr.total != list.totalParts) {
					System.err.println("Received a LIST with total "+list.totalParts+", which is different than previous ones (expected "+pr.total+")");
					pending.remove(list.sender);
					continue;
				}
			}

			// partNum is already checked by Message.List
			pr.receive(list.partNum, list.data);

			if (pr.done()) {
				pending.remove(list.sender);
				try {
					peerTable.synchronize(pr.peer, pr.data(), pr.seqNum);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
			}
		}
	}
}
