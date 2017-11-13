import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.List;
import java.io.OutputStream;
import java.io.PrintStream;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

class Dumper implements Runnable {
	private ServerSocket servSocket;
	private PeerTable peerTable;
	private Database database;
	private static final int BACKLOG_SIZE = 3;

	public Dumper(int port, PeerTable pt, Database db) {
		try {
			this.servSocket = new ServerSocket(port, BACKLOG_SIZE);
			this.peerTable = pt;
			this.database = db;
		} catch (IOException e){
			e.printStackTrace();
		}
	}

	class ClientHandler implements Runnable {
		private Socket client;

		ClientHandler(Socket client) {
			this.client = client;
		}

		private String usage() {
			return "Usage: [Command]\n"
							+ "Command:\n"
							+ "\ta, all         : print databse, peerTable\n"
							+ "\tpt, peertable  : display the peerTable\n"
							+ "\tdb, database   : display the databse\n"
							+ "\th, help        : display this usage usage\n\n";
		}

		private String prettyPeerTable() {
			String msg = "# Peer Table #\n";
			msg += "+------------------------------------------------+\n";
			msg += String.format("| %1$16s | %2$13s | %3$11s |\n", "Id", "State", "Seq#");
			msg += "+------------------------------------------------+\n";
			List<PeerTable.Record> records = peerTable.records();
			for (PeerTable.Record e : records) {
				msg += String.format("| %1$16s | %2$13s | %3$11s |\n", e.id, e.state(), e.seqNum());
			}
			msg += "+------------------------------------------------+\n\n";
			return msg;
		}


		/* Tinyfied 64 chars database */
		private String prettyDatabase() {
			String msg = "# Database - Id : " + database.seqNum() + " #\n";
			msg += "+------------------------------------------------------------------+\n";
			List<String> db = database.data();
			for (String s : db) {
				msg += String.format("| %1$64s |\n", s);
			}
			msg += "+------------------------------------------------------------------+\n\n";
			return msg;
		}

		private void handleMessage(PrintStream ps, String cmd) {
			cmd = cmd.toLowerCase();
			if (cmd.equals("all") || cmd.equals("a")) {
				ps.print(this.prettyPeerTable());
				ps.print(prettyDatabase());
				return;
			}
			if (cmd.equals("peertable") || cmd.equals("pt")) {
				ps.print(this.prettyPeerTable());
				return;
			}
			if (cmd.equals("peerdatabase") || cmd.equals("pdb")) {
				ps.print("Not yet implemented : " + cmd + "\n");
			}
			if (cmd.equals("database") || cmd.equals("db")) {
				ps.print(prettyDatabase());
				return;
			}
			if (cmd.equals("help") || cmd.equals("h")) {
				ps.print(this.usage());
				return;
			}
			if (cmd.equals("quit") || cmd.equals("q")) {
				try {
					client.close();
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			ps.print("Unknown command : "+cmd+"\n");
		}

		public void run() {
			BufferedReader br;
			PrintStream ps;
			try {
				InputStream is = client.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				br = new BufferedReader(isr);
				OutputStream os = client.getOutputStream();
				ps = new PrintStream(os);
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

			while (true) {
				try {
					String cmd = br.readLine();
					handleMessage(ps, cmd);
					ps.flush();
				} catch (IOException e){
					return;
				}
			}
		}
	}

	public void run() {
		while (true) {
			try {
				Socket client = servSocket.accept();
				new Thread(new ClientHandler(client)).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
