/**
 * Periodically sends synchronization requests to unsynchronized peers.
 */
class SynSender implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private String local;
	private int synInterval;

	public SynSender(MuxDemux muxDemux, PeerTable peerTable, String local, int synInterval) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.local = local;
		this.synInterval = synInterval;
	}

	public void handleMessage(Envelope msg) {
		// No-op
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(synInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}

			for (PeerTable.Record rec : peerTable.records()) {
				if (rec.state() != PeerTable.State.SYNCHRONIZED) {
					Message.Syn syn = new Message.Syn(local, rec.id, rec.lastSeqNum());
					Envelope env = new Envelope(peer.address, syn);
					muxDemux.send(env);
				}
			}
		}
	}
}
