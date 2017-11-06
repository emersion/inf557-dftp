import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class DebugReceiver implements MessageHandler, Runnable {
	BlockingQueue<Envelope> incoming = new ArrayBlockingQueue<>(32);

	public DebugReceiver() {}

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

			System.out.println(env);
		}
	}
}
