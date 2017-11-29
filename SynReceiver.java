import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Asks the `ListSender` to send LIST messages when a SYN is received.
 */
class SynReceiver implements MessageHandler, Runnable {
	private MuxDemux muxDemux;
	private PeerTable peerTable;
	private ListSender listSender;
	private String local;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public SynReceiver(MuxDemux muxDemux, PeerTable peerTable, ListSender listSender, String local) {
		this.muxDemux = muxDemux;
		this.peerTable = peerTable;
		this.listSender = listSender;
		this.local = local;
	}

	public void handleMessage(Envelope env) {
		if (!(env.msg instanceof Message.Syn)) {
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
			Message.Syn syn = (Message.Syn)env.msg;

			if (!local.equals(syn.peer)) {
				continue; // Not for me
			}

			try {
				peerTable.update(syn.sender, env.address, syn.seqNum);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			listSender.sendTo(env.address, syn.sender);
		}
	}
}
