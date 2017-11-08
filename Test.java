import java.net.DatagramSocket;
import java.net.InetAddress;

class Test {
	private static final int port = 4242;
	private static final int helloInterval = 1;

	public static void main(String[] args) throws Exception {
		String local = InetAddress.getLocalHost().getHostName();

		DatagramSocket socket = new DatagramSocket(port);
		MuxDemux muxDemux = new MuxDemux(socket);
		PeerTable peerTable = new PeerTable();
		Database db = new Database();

		HelloReceiver helloReceiver = new HelloReceiver(peerTable);
		new Thread(helloReceiver).start();
		muxDemux.addHandler(helloReceiver);

		HelloSender helloSender = new HelloSender(muxDemux, peerTable, db, local, helloInterval);
		new Thread(helloSender).start();
		muxDemux.addHandler(helloSender);

		DebugReceiver debugReceiver = new DebugReceiver();
		new Thread(debugReceiver).start();
		muxDemux.addHandler(debugReceiver);

		muxDemux.run();
	}
}
