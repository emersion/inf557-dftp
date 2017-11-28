import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class ListSender implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
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

	public ListSender(MuxDemux muxDemux, Database db, String local) {
		this.muxDemux = muxDemux;
		this.db = db;
		this.local = local;
	}

	public void handleMessage(Envelope msg) {
		// No-op
	}

	/**
	 * Requests to send the local database to a remote peer.
	 */
	public synchronized void sendTo(InetAddress address, String id) {
		// check if already queued
		for (Request req : outgoing){
			if (address.equals(req.address) && id.equals(req.id)) {
				return;
			}
		}

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

			// TODO: if the database is updated between those two calls, we end up in
			// an inconsistent state
			int seqNum = db.seqNum();
			List<String> data = db.data();
			for (int i = 0; i < data.size(); ++i) {
				Message.List list = new Message.List(local, req.id, seqNum, data.size(), i, data.get(i));
				Envelope env = new Envelope(req.address, list);
				muxDemux.send(env);
			}
		}
	}
}
