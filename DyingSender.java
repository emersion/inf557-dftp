class DyingSender implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private Database db;
	private String local;
	private int dyingInterval;
	private int dyingCount;

	public DyingSender(MuxDemux muxDemux, String local, int dyingInterval, int dyingCount) {
		this.muxDemux = muxDemux;
		this.local = local;
		this.dyingInterval = dyingInterval;
		this.dyingCount = dyingCount;
	}

	public void handleMessage(Envelope msg) {
		// No-op
	}

	public void run() {
		System.out.println("Got SIGINT, sending DYING messages...");

		for (int i = 0; i < dyingCount; ++i) {
			Message.Dying dying = new Message.Dying(local);
			muxDemux.broadcast(dying);

			try {
				Thread.sleep(dyingInterval);
			} catch (InterruptedException e) {
				break;
			}
		}
	}
}
