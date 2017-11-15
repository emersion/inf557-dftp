import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.net.DatagramSocket;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.UnknownHostException;

class MuxDemux implements Runnable {
	private static final int RECEIVE_BUFFER_SIZE = 2048;

	private DatagramSocket socket;
	private final InetAddress brd;
	private BlockingQueue<Envelope> outgoing = new ArrayBlockingQueue<>(32);
	private List<MessageHandler> handlers = new ArrayList<>();

	public MuxDemux(DatagramSocket socket) throws SocketException, UnknownHostException {
		socket.setBroadcast(true);
		this.socket = socket;
		this.brd = InetAddress.getByName("255.255.255.255");
	}

	public void addHandler(MessageHandler h) {
		this.handlers.add(h);
	}

	private class SenderWorker implements Runnable {
		public void run() {
			while (true) {
				Envelope env;
				try {
					env = outgoing.take();
				} catch (InterruptedException e) {
					break;
				}

				byte[] buf = env.msg.format().getBytes();
				DatagramPacket packet = new DatagramPacket(buf, buf.length, env.address, socket.getLocalPort());
				try {
					socket.send(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private class ReceiverWorker implements Runnable {
		public void run() {
			// TODO: UTF-8 messages can be truncated in the middle of a codepoint
			byte[] buf = new byte[RECEIVE_BUFFER_SIZE];
			DatagramPacket packet = new DatagramPacket(buf, buf.length);
			while (true) {
				try {
					socket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}

				String raw = new String(packet.getData(), 0, packet.getLength());
				InetAddress addr = packet.getAddress();

				Message msg;
				try {
					msg = Message.parse(raw);
				} catch (Exception e) {
					System.err.println("Error while parsing message from "+addr+": "+raw);
					e.printStackTrace();
					continue;
				}

				Envelope e = new Envelope(addr, msg);
				for (MessageHandler h : handlers) {
					h.handleMessage(e);
				}
			}
		}
	}

	public void run() {
		new Thread(new SenderWorker()).start();
		new ReceiverWorker().run();
		socket.close();
	}

	public void broadcast(Message msg) {
		outgoing.offer(new Envelope(brd, msg));
	}

	public void send(Envelope env) {
		outgoing.add(env);
	}
}
