import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class HelloReceiver implements MessageHandler, Runnable {
	PeerTable peerTable;
	BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public HelloReceiver(PeerTable peerTable) {
		this.peerTable = peerTable;
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

			if (!(env.msg instanceof Message.Hello)) {
				continue;
			}
			Message.Hello hello = (Message.Hello)env.msg;

			peerTable.update(hello.sender, env.address, hello.seqNum);
		}
	}
}
