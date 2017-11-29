import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maintains a database of all file paths in a given directory. The database is
 * updated periodically.
 *
 * This class has ownership over the database it holds.
 */
public class DbUpdater implements Runnable {
	private File sharedFolder;
	private int updateInterval;

	private Database db = null;

	public DbUpdater(String sharedFolderPath, int updateInterval) {
		if (sharedFolderPath == null) {
			throw new InvalidParameterException("invalid argument path is null");
		}
		if (updateInterval <= 0) {
			throw new InvalidParameterException("invalid argument interval is not positive");
		}

		sharedFolder = new File(sharedFolderPath);
		if (!sharedFolder.isDirectory()) {
			throw new InvalidParameterException("path provided is not a directory");
		}

		this.updateInterval = updateInterval;

		// Initial DB update
		scan();
	}

	public Database database() {
		return db;
	}

	/**
	 * Traverses `dirPath` and recursively adds all file paths to `paths`.
	 */
	private Set<String> getListOfPath(Set<String> paths, String dirPath) {
		File currentDir = new File(dirPath);
		if (!currentDir.isDirectory()) {
			throw new IllegalArgumentException("wrong dir path");
		}

		for (File f: currentDir.listFiles()) {
			if (f.isFile()) {
				paths.add(sharedFolder.toPath().relativize(f.toPath()).toString());
			}

			if (f.isDirectory()) {
				paths.addAll(getListOfPath(new HashSet<String>(), f.getPath())) ;
			}
		}
		return paths;
	}

	/**
	 * Updates the database with a new list of paths. If the database doesn't
	 * exist it's created.
	 */
	private void update(Set<String> paths) {
		String[] pathsArray = paths.toArray(new String[paths.size()]);
		if (db == null) {
			db = new Database(pathsArray, 0);
		} else {
			db.update(pathsArray);
		}
	}

	/**
	 * Scans the directory and updates the database if necessary.
	 */
	private void scan() {
		Set<String> paths = getListOfPath(new HashSet<String>(), sharedFolder.getPath());
		if (db == null) {
			update(paths);
			return;
		}

		List<String> currentDb = db.data();

		if (currentDb.size() != paths.size()) {
			update(paths);
			return;
		}

		for (String path : currentDb) {
			if (!paths.contains(path)) {
				update(paths);
				return;
			}
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(updateInterval * 1000);
			} catch (InterruptedException e) {
				break;
			}

			scan();
		}
	}
}
