import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.io.OutputStream;
import java.io.PrintStream;

class PeerTableDumper implements Runnable {
	private ServerSocket servSocket;
	private PeerTable peerTable;
	private static final int BACKLOG_SIZE = 3;

	public PeerTableDumper(int port, PeerTable pt) {
		try {
			this.servSocket = new ServerSocket(port, BACKLOG_SIZE);
			this.peerTable = pt;
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	private String makeFancyMessage() {
		// %[argument_index$][flags][width][.precision]conversion
		String msg = "+------------------------------------------------+\n";
		msg += String.format("| %1$16s | %2$13s | %3$11s |\n", "Id", "State", "Seq#");
		msg += "+------------------------------------------------+\n";
		List<PeerTable.Record> records = peerTable.records();
		for (PeerTable.Record e : records) {
			msg += String.format("| %1$16s | %2$13s | %3$11s |\n", e.id, e.state(), e.lastSeqNum);
		}
		msg += "+------------------------------------------------+\n";
		return msg;
	}

	public void run() {
		while (true) {
			try {
				Socket client = servSocket.accept();
				OutputStream os = client.getOutputStream();
				PrintStream ps = new PrintStream(os);
				ps.print(this.makeFancyMessage());
				ps.flush();
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
