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

			try {
				peerTable.update(syn.sender, env.address, syn.seqNum);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}

			listSender.sendTo(env.address, syn.peer);
		}
	}
}
