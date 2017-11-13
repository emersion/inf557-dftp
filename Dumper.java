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

	private class ClientHandler implements Runnable {
		private Socket client;

		ClientHandler(Socket client) {
			this.client = client;
		}

		private String usage() {
			return "Usage: [Command]\n"
				+ "Command:\n"
				+ "\ta, All            : print databse, peerTable\n"
				+ "\tpt, PeerTable     : display the peerTable\n"
				+ "\tpdb, PeerDatabse  : display the peerTable\n"
				+ "\tdb, Database      : display the databse\n"
				+ "\th, Help           : display this usage usage\n\n";
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

		/* Tinyfied 83 chars database (83 is about half a screen size) */
		private String prettyDatabase() {
			String msg = "# Database - Id : " + database.seqNum() + " #\n";
			msg += "+-------------------------------------------------------------------------------------+\n";
			List<String> db = database.data();
			for (String s : db) {
				msg += String.format("| %1$83s |\n", s);
			}
			msg += "+-------------------------------------------------------------------------------------+\n\n";
			return msg;
		}

		private String prettyPeerDatabase() {
			String msg = "## Peer DataBases ##\n";
			msg += "+-------------------------------------------------------------------------------------+\n";
			msg += String.format("| %1$16s | %2$64s |\n", "Peer Id", "Data");
			msg += "+-------------------------------------------------------------------------------------+\n";
			List<PeerTable.Record> records = peerTable.records();
			for (PeerTable.Record e : records) {
				if (e.database() != null) {
					msg += String.format("| %1$16s | %2$64s |\n", e.id, e.database().data());
				}
			}
			msg += "+-------------------------------------------------------------------------------------+\n\n";
			return msg;
		}

		private void handleMessage(PrintStream ps, String cmd) {
			cmd = cmd.toLowerCase();
			switch(cmd) {
				case "all": case "a":
				ps.print(prettyPeerTable());
				ps.print(prettyPeerDatabase());
				ps.print(prettyDatabase());
				break;

				case "peertable": case "pt":
				ps.print(prettyPeerTable());
				break;

				case "peerdatabase": case "pdb":
				ps.print(prettyPeerDatabase());
				break;

				case "database": case "db":
				ps.print(prettyDatabase());
				break;

				case "help": case "h":
				ps.print(usage());
				break;

				case "quit": case "q":
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

				default:
				ps.print("Unknown command : "+cmd+"\n");
				break;
			}
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
					ps.print(">");
					String cmd = br.readLine();
					if (cmd == null){
						break;
					}
					handleMessage(ps, cmd);
					ps.flush();
				} catch (IOException e){
					e.printStackTrace();
					break;
				}
			}
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
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
