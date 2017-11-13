import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class SynReceiver implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private ListSender listSender;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public SynReceiver(MuxDemux muxDemux, PeerTable peerTable, ListSender listSender) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.listSender = listSender;
	}

	public void handleMessage(Envelope env) {
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

			if (!(env.msg instanceof Message.Syn)) {
				continue;
			}
			Message.Syn syn = (Message.Syn)env.msg;

			listSender.sendTo(env.address, syn.peer);
		}
	}
}
