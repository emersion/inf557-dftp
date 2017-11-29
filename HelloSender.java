/**
 * Periodically broadcasts HELLO messages.
 */
class HelloSender implements Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private Database db;
	private String local;
	private int helloInterval;

	public HelloSender(MuxDemux muxDemux, PeerTable peerTable, Database db, String local, int helloInterval) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.db = db;
		this.local = local;
		this.helloInterval = helloInterval;
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(helloInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}

			Message.Hello hello = new Message.Hello(local, db.seqNum(), helloInterval);
			for (PeerTable.Record rec : peerTable.records()) {
				if (rec.state() != PeerTable.State.DYING) {
					hello.addPeer(rec.id);
				}
			}
			muxDemux.broadcast(hello);
		}
	}
}
