import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class FileDownloader implements Runnable {
	private static final int CONNECT_TIMEOUT = 30;
	private static final int READ_TIMEOUT = 10;

	private int port;
	private Path directory;
	private PeerTable peerTable;

	private BlockingQueue<Request> requests = new ArrayBlockingQueue<>(32);

	public FileDownloader(int port, Path directory, PeerTable peerTable) {
		this.port = port;
		this.directory = directory;
		this.peerTable = peerTable;
	}

	private static class Request {
		public final String peer;
		public final String filename;

		public Request(String peer, String filename) {
			this.peer = peer;
			this.filename = filename;
		}
	}

	public void download(String peer, String filename) {
		// Check if already queued
		for (Request req : requests) {
			if (peer.equals(req.peer) && filename.equals(req.filename)) {
				return;
			}
		}

		requests.offer(new Request(peer, filename));
	}

	/**
	 * Copies data from r to w with a buffer.
	 *
	 * TODO: if EOF is encountered before reading len bytes, no error is raised.
	 * @param  Writer      w   The writer to write data to.
	 * @param  Reader      r   The reader to read data from.
	 * @param  int         len If > 0, the number of bytes to copy.
	 * @throws IOException
	 */
	private static void copy(Writer w, Reader r, int len) throws IOException {
		// TODO: it's dumb to use a CharBuffer since the response body can be binary
		// data, it would be better to use a BytesBuffer. However, I didn't find a
		// way to read the response header line by line *and* to read the response
		// body as bytes, because the BufferedReader buffers the begining of the
		// response body.
		CharBuffer buf = CharBuffer.allocate(2048);
		int remaining = len;

		while (true) {
			if (len >= 0 && remaining == 0) {
				break;
			}

			buf.clear();

			int limit = buf.capacity();
			if (len >= 0 && remaining < limit) {
				limit = remaining;
			}

			int n = r.read(buf.subSequence(0, limit));
			if (n < 0) {
				break;
			}
			w.write(buf.array(), 0, n);

			if (len >= 0) {
				remaining -= n;
			}
		}
	}

	private void doDownload(Request req) throws Exception {
		PeerTable.Record rec = peerTable.get(req.peer);
		if (rec == null) {
			throw new RuntimeException("requested a file download from an unknown peer: "+req.peer);
		}
		if (rec.state() == PeerTable.State.DYING) {
			throw new RuntimeException("requested a file download from a dying peer: "+req.peer);
		}

		Path peerDir = directory.resolve(req.peer);
		Path filepath = peerDir.resolve(req.filename).normalize();
		if (!filepath.startsWith(peerDir)) {
			throw new RuntimeException("filename "+req.filename+" resolved outside peer directory");
		}
		File parent = filepath.getParent().toFile();
		if (!parent.isDirectory()) {
			parent.mkdirs();
		}

		InetSocketAddress addr = new InetSocketAddress(rec.address, port);
		Socket socket = new Socket();
		socket.setSoTimeout(READ_TIMEOUT*1000);
		socket.connect(addr, CONNECT_TIMEOUT*1000);

		try {
			OutputStream os = socket.getOutputStream();
			BufferedReader r = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			PrintWriter w = new PrintWriter(os);
			w.format("get %s\n", req.filename);
			w.flush();

			String filename = r.readLine();
			if (filename == null) {
				throw new RuntimeException("invalid file: connection closed by remote");
			}
			String sizeStr = r.readLine();
			if (sizeStr == null) {
				throw new RuntimeException("connection closed by remote when reading size");
			}

			int size;
			try {
				size = Integer.parseInt(sizeStr);
			} catch (NumberFormatException e) {
				throw new RuntimeException("invalid file size: "+e.getMessage());
			}

			FileOutputStream fos = new FileOutputStream(filepath.toString());
			OutputStreamWriter osw = new OutputStreamWriter(fos);
			try {
				copy(osw, r, size);
			} finally {
				osw.close();
				fos.close();
			}
		} finally {
			socket.close();
		}
	}

	public void run() {
		while (true) {
			Request req;
			try {
				req = requests.take();
			} catch (InterruptedException e) {
				break;
			}

			try {
				System.out.println("Downloading "+req.filename+" from "+req.peer);
				doDownload(req);
			} catch (Exception e) {
				System.err.println("Error downloading "+req.filename+" from "+req.peer);
				e.printStackTrace();
			}
		}
	}
}
