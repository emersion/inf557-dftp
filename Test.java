import java.net.DatagramSocket;

class Test {
	private static final int port = 4242;
	private static final String local = "kurisu";
	private static final int helloInterval = 1;

	public static void main(String[] args) throws Exception {
		DatagramSocket socket = new DatagramSocket(port);
		MuxDemux muxDemux = new MuxDemux(socket);
		PeerTable peerTable = new PeerTable();

		HelloReceiver helloReceiver = new HelloReceiver(peerTable);
		new Thread(helloReceiver).start();
		muxDemux.addHandler(helloReceiver);

		HelloSender helloSender = new HelloSender(muxDemux, peerTable, local, helloInterval);
		new Thread(helloSender).start();
		muxDemux.addHandler(helloSender);

		DebugReceiver debugReceiver = new DebugReceiver();
		new Thread(debugReceiver).start();
		muxDemux.addHandler(debugReceiver);

		muxDemux.run();
	}
}
