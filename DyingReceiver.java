import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class DyingReceiver implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public DyingReceiver(MuxDemux muxDemux, PeerTable peerTable) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
	}

	public void handleMessage(Envelope env) {
		if (!(env.msg instanceof Message.Dying)) {
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
			Message.Dying dying = (Message.Dying)env.msg;

			try {
				peerTable.die(dying.sender, env.address);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
}
