import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

class Test {
	private static final int port = 4242;
	private static final int helloInterval = 3;
	private static final int synInterval = 5;

	private static String local() throws UnknownHostException {
		String local = InetAddress.getLocalHost().getHostName();
		local = local.replaceAll("[^a-zA-Z0-9]", "");
		if (local.length() > 16) {
			local = local.substring(0, 16);
		}
		return local;
	}

	public static void main(String[] args) throws Exception {
		String local = local();

		DatagramSocket socket = new DatagramSocket(port);
		MuxDemux muxDemux = new MuxDemux(socket);
		PeerTable peerTable = new PeerTable();
		Database db = new Database(new String[]{"Hello", "World"}, 0);

		HelloReceiver helloReceiver = new HelloReceiver(peerTable);
		new Thread(helloReceiver).start();
		muxDemux.addHandler(helloReceiver);

		HelloSender helloSender = new HelloSender(muxDemux, peerTable, db, local, helloInterval);
		new Thread(helloSender).start();
		muxDemux.addHandler(helloSender);

		ListSender listSender = new ListSender(muxDemux, peerTable, db, local);
		new Thread(listSender).start();
		muxDemux.addHandler(listSender);

		ListReceiver listReceiver = new ListReceiver(peerTable, local);
		new Thread(listReceiver).start();
		muxDemux.addHandler(listReceiver);

		SynSender synSender = new SynSender(muxDemux, peerTable, local, synInterval);
		new Thread(synSender).start();
		muxDemux.addHandler(synSender);

		SynReceiver synReceiver = new SynReceiver(muxDemux, peerTable, listSender, local);
		new Thread(synReceiver).start();
		muxDemux.addHandler(synReceiver);

		DebugReceiver debugReceiver = new DebugReceiver();
		new Thread(debugReceiver).start();
		muxDemux.addHandler(debugReceiver);

		Dumper dumper = new Dumper(port, peerTable, db);
		new Thread(dumper).start();

		muxDemux.run();
	}
}
