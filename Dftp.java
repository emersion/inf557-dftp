import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;

class Dftp {
	private static final int port = 4242;
	private static final int helloInterval = 1;
	private static final int synInterval = 1;
	private static final int dirScanInterval = 10;
	private static final int scheduleInterval = 1;
	private static final int dyingInterval = 100;
	private static final int dyingCount = 3;
	private static final Path sharedDir = Paths.get("shared/");

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
		Path localDir = sharedDir.resolve(local);

		// Ensure the local directory exists
		new File(localDir.toString()).mkdirs();

		DatagramSocket socket = new DatagramSocket(port);
		MuxDemux muxDemux = new MuxDemux(socket);
		PeerTable peerTable = new PeerTable();

		DbUpdater dbUpdater = new DbUpdater(localDir.toString(), dirScanInterval);
		Database db = dbUpdater.database();
		new Thread(dbUpdater).start();

		HelloReceiver helloReceiver = new HelloReceiver(peerTable);
		new Thread(helloReceiver).start();
		muxDemux.addHandler(helloReceiver);

		HelloSender helloSender = new HelloSender(muxDemux, peerTable, db, local, helloInterval);
		new Thread(helloSender).start();

		ListSender listSender = new ListSender(muxDemux, db, local);
		new Thread(listSender).start();

		ListReceiver listReceiver = new ListReceiver(peerTable, local);
		new Thread(listReceiver).start();
		muxDemux.addHandler(listReceiver);

		SynSender synSender = new SynSender(muxDemux, peerTable, local, synInterval);
		new Thread(synSender).start();

		SynReceiver synReceiver = new SynReceiver(muxDemux, peerTable, listSender, local);
		new Thread(synReceiver).start();
		muxDemux.addHandler(synReceiver);

		DebugReceiver debugReceiver = new DebugReceiver();
		new Thread(debugReceiver).start();
		muxDemux.addHandler(debugReceiver);

		// TODO: stop all other senders when shutting down
		DyingSender dyingSender = new DyingSender(muxDemux, local, dyingInterval, dyingCount);
		Runtime.getRuntime().addShutdownHook(new Thread(dyingSender));

		DyingReceiver dyingReceiver = new DyingReceiver(muxDemux, peerTable);
		new Thread(dyingReceiver).start();
		muxDemux.addHandler(dyingReceiver);

		FileDownloader fileDownloader = new FileDownloader(port, sharedDir, peerTable);
		new Thread(fileDownloader).start();

		Dumper dumper = new Dumper(port, localDir, peerTable, db, fileDownloader);
		new Thread(dumper).start();

		DownloadScheduler downloadScheduler = new DownloadScheduler(peerTable, fileDownloader, scheduleInterval);
		new Thread(downloadScheduler).start();

		muxDemux.run();
	}
}
