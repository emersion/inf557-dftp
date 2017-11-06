class HelloSender implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private String local;
	private int helloInterval;

	public HelloSender(MuxDemux muxDemux, PeerTable peerTable, String local, int helloInterval) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.local = local;
		this.helloInterval = helloInterval;
	}

	public void handleMessage(Envelope msg) {
		// No-op
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(this.helloInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}

			// TODO: seqNum
			Message.Hello hello = new Message.Hello(this.local, 0, this.helloInterval);
			for (PeerTable.Record rec : peerTable.records().values()) {
				if (rec.state() != PeerTable.State.DYING) {
					hello.addPeer(rec.id);
				}
			}
			this.muxDemux.broadcast(hello);
		}
	}
}
