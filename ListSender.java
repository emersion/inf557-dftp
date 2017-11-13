import java.net.InetAddress;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class ListSender implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private Database db;
	private String local;

	private BlockingQueue<Request> outgoing = new ArrayBlockingQueue<>(32);

	private static class Request {
		public final InetAddress address;
		public final String id;

		public Request(InetAddress address, String id) {
			this.address = address;
			this.id = id;
		}
	}

	public ListSender(MuxDemux muxDemux, PeerTable peerTable, Database db, String local) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.db = db;
		this.local = local;
	}

	public void handleMessage(Envelope msg) {
		// No-op
	}

	public synchronized void sendTo(InetAddress address, String id) {
		// TODO: check if already queued
		outgoing.offer(new Request(address, id));
	}

	public void run() {
		while (true) {
			Request req;
			try {
				req = outgoing.take();
			} catch (InterruptedException e) {
				break;
			}

			int seqNum = db.seqNum();
			String[] data = db.data();
			for (int i = 0; i < data.length; ++i) {
				Message.List list = new Message.List(local, req.id, seqNum, data.length, i, data[i]);
				Envelope env = new Envelope(req.address, list);
				muxDemux.send(env);
			}
		}
	}
}
