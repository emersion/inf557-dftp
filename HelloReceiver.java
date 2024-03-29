import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Updates the peer table when HELLO messages are received.
 */
class HelloReceiver implements MessageHandler, Runnable {
	private PeerTable peerTable;
	private String local;

	private BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public HelloReceiver(PeerTable peerTable, String local) {
		this.peerTable = peerTable;
		this.local = local;
	}

	public void handleMessage(Envelope env) {
		if (!(env.msg instanceof Message.Hello)) {
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
			Message.Hello hello = (Message.Hello)env.msg;

			if (local.equals(hello.sender)) {
				continue; // It's our own HELLO
			}

			try {
				peerTable.update(hello.sender, env.address, hello.seqNum);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
		}
	}
}
