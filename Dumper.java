import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class Dumper implements Runnable {
	private static final int BACKLOG_SIZE = 3;

	private ServerSocket servSocket;
	private Path myPath;
	private PeerTable peerTable;
	private Database database;
	private FileDownloader fileDownloader;

	public Dumper(int port, Path myPath, PeerTable pt, Database db, FileDownloader fileDownloader) throws IOException {
		this.servSocket = new ServerSocket(port, BACKLOG_SIZE);
		this.myPath = myPath;
		this.peerTable = pt;
		this.database = db;
		this.fileDownloader = fileDownloader;
	}

	private class ClientHandler implements Runnable {
		private Socket client;
		private String lastCmd = null;

		public ClientHandler(Socket client) {
			this.client = client;
		}

		private String usage() {
			return "Usage: <command>\n"
				+ "Commands:\n"
				+ "\ta, all                         print database, peerTable\n"
				+ "\tpt, peertable                  display the peerTable\n"
				+ "\tpadb, peerAllDatabase          display all peer databases\n"
				+ "\tpdb, peerDatabase <peer>       display a peer's database\n"
				+ "\tdb, database                   display the database\n"
				+ "\tudb, updateDatabase <e1,...>   update the local database\n"
				+ "\tget <file>                     get a local file\n"
				+ "\tpg, peerget <peer> <file>      download a remote file\n"
				+ "\tq, quit                        quit this console\n"
				+ "\th, help                        display this usage\n";
		}

		private String prettyPeerTable() {
			String msg = "Peer Table\n";
			msg += "+------------------------------------------------+\n";
			msg += String.format("| %1$16s | %2$13s | %3$11s |\n", "ID", "State", "Seq#");
			msg += "+------------------------------------------------+\n";
			List<PeerTable.Record> records = peerTable.records();
			for (PeerTable.Record e : records) {
				msg += String.format("| %1$16s | %2$13s | %3$11s |\n", e.id, e.state(), e.seqNum());
			}
			msg += "+------------------------------------------------+\n\n";
			return msg;
		}

		/* Tinyfied 83 chars database (83 is about half a screen size) */
		private String prettyDatabase(Database database) {
			String msg = "Database - Sequence Number: " + database.seqNum() + "\n";
			if (database == null) {
				msg = "Database not present\n";
				return msg;
			}
			msg += "+-------------------------------------------------------------------------------------+\n";
			List<String> db = database.data();
			for (String s : db) {
				msg += String.format("| %1$83s |\n", s);
			}
			msg += "+-------------------------------------------------------------------------------------+\n\n";
			return msg;
		}

		private String prettyAllPeerDatabase() {
			String msg = "Peer Databases\n";
			msg += "+-------------------------------------------------------------------------------------+\n";
			msg += String.format("| %1$16s | %2$64s |\n", "Peer ID", "Data");
			msg += "+-------------------------------------------------------------------------------------+\n";
			List<PeerTable.Record> records = peerTable.records();
			for (PeerTable.Record e : records) {
				Database db = e.database();
				if (db != null) {
					String data = db.data().toString();
					if (data.length() > 64) {
						data = data.substring(0,60);
						data += "...]";
					}
					msg += String.format("| %1$16s | %2$64s |\n", e.id, data);
				}
			}
			msg += "+-------------------------------------------------------------------------------------+\n\n";
			return msg;
		}

		private String prettyPeerDatabase(String peerId) {
			String msg = "";
			PeerTable.Record peer = peerTable.get(peerId);
			if (peer == null) {
				msg = "No such peerId\n";
			} else {
				msg = prettyDatabase(peer.database());
			}
			return msg;
		}

		private void updateDatabase(String[] data) {
			database.update(data);
		}

		private void handleGetFile(String filename) {
			OutputStream os;
			try {
				os = this.client.getOutputStream();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
			PrintStream ps = new PrintStream(os);
			// find the file: filename
			Path dir = myPath.resolve(filename);
			Path path = dir.normalize();
			if (!path.startsWith(myPath)) {
				ps.print("Get out of here !\n");
				return;
			}

			File f = path.toFile();
			if (f.isFile()) {
				ps.print(filename + "\n");
				ps.print(f.length() + "\n");
				ps.flush();
				try {
					Files.copy(path, os);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				client.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private void handleMessage(PrintStream ps, String cmd) {
			String[] cmdList = cmd.split(" ");
			switch(cmdList[0].toLowerCase()) {
			case "all":
			case "a":
				ps.print(prettyPeerTable());
				ps.print(prettyAllPeerDatabase());
				break;

			case "peertable":
			case "pt":
				ps.print(prettyPeerTable());
				break;

			case "peeralldatabase":
			case "padb":
				ps.print(prettyAllPeerDatabase());
				break;

			case "peerdatabase":
			case "pdb":
				if (cmdList.length > 1) {
					ps.print(prettyPeerDatabase(cmdList[1]));
				} else {
					ps.print("Wrong number of argument. pdb usage: pdb <peer>\n");
				}
				break;

			case "database":
			case "db":
				ps.print(prettyDatabase(database));
				break;

			case "updatedatabase":
			case "udb":
				if (cmdList.length > 1) {
					String[] dbList = cmdList[1].split(",");
					updateDatabase(dbList);
				} else {
					ps.print("No list to update, udb usage: udb [e1,...]\n");
				}
				break;

			case "get":
				if (cmdList.length > 1) {
					handleGetFile(cmdList[1]);
				} else {
					ps.print("No filename specified, get usage: get <filename>\n");
				}
				break;

			case "peerget":
			case "pg":
				if (cmdList.length > 2) {
					fileDownloader.download(cmdList[1], cmdList[2]);
				} else {
					ps.print("Wrong number of argument. pg usage: pg <peer> <file>\n");
				}
				break;

			case "help":
			case "h":
				ps.print(usage());
				break;

			case "quit":
			case "q":
				try {
					client.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;

			case "":
				if (lastCmd != null) {
					handleMessage(ps, lastCmd);
				}
				break;

			default:
				ps.print("Unknown command : "+cmd+"\n");
				break;
			}

			if (!cmd.isEmpty()) {
				lastCmd = cmd;
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
					// ps.print("> ");
					// ps.flush();
					String cmd = br.readLine();
					if (cmd == null) {
						break;
					}
					handleMessage(ps, cmd);
					if (client.isClosed()) {
						break;
					}
					ps.flush();
				} catch (IOException e) {
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
