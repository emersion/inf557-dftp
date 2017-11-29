import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Schedules file downloads from other peers. It downloads EVERYTHING.
 */
class DownloadScheduler implements Runnable {
	private PeerTable peerTable;
	private FileDownloader fileDownloader;
	private int scheduleInterval;

	/**
	 * For each peer ID, contains the last processed sequence number.
	 */
	private Map<String, Integer> processed = new HashMap<>();

	public DownloadScheduler(PeerTable peerTable, FileDownloader fileDownloader, int scheduleInterval) {
		this.peerTable = peerTable;
		this.fileDownloader = fileDownloader;
		this.scheduleInterval = scheduleInterval;
	}

	public void run() {
		while (true) {
			// TODO: cleanup entries in this.processed

			try {
				Thread.sleep(scheduleInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}

			for (PeerTable.Record rec : peerTable.records()) {
				Database db = rec.database();
				if (db == null) {
					continue;
				}

				int seqNum = db.seqNum();
				Integer lastSeqNum = processed.get(rec.id);
				if (lastSeqNum != null && seqNum == lastSeqNum) {
					continue; // Database hasn't changed
				}

				// Database has changed, re-download EVERYTHING (could be a modified
				// file)
				System.out.println("Scheduling download of "+rec.id+" @ "+seqNum);
				List<String> data = db.data();
				for (String filename : data) {
					fileDownloader.download(rec.id, filename);
				}

				processed.put(rec.id, seqNum);
			}
		}
	}
}
