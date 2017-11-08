import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.io.OutputStream;
import java.io.PrintStream;

class PeerTableDumper implements Runnable {
	private ServerSocket servSocket;
	private PeerTable peerTable;
  private final int default_backlog_size = 3;

  public PeerTableDumper(int port, PeerTable pt) {
		try {
			this.servSocket = new ServerSocket(port, default_backlog_size);
			this.peerTable = pt;
		} catch (IOException e){
			e.printStackTrace();
		}
  }

	private String makeFancyMessage() {
		String msg = "----------------------------\n";
		msg += "Id  | State | seq# \n";
		List<PeerTable.Record> records = peerTable.records();
		for (PeerTable.Record e : records) {
			msg += "| " + e.id + " | " + e.state() + " | " + e.lastSeqNum + " \n";
		}
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
