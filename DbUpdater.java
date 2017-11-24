import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by sathouel on 22/11/2017.
 */
public class DbUpdater implements Runnable {

    private File sharedFolder;
    private Database db;
    private int updateInterval;

    public DbUpdater(Database database, String sharedFolderPath, int interval){

        if (sharedFolderPath == null) {
            throw new InvalidParameterException("invalid argument path is null");
        }

        if (interval <= 0) {
            throw new InvalidParameterException("invalid argument interval is not positive");
        }

        if (database == null) {
            throw new InvalidParameterException("invalid argument database is null");
        }

        sharedFolder = new File(sharedFolderPath);

        if (!sharedFolder.isDirectory()) {
            throw new InvalidParameterException("path provided is not a directory");
        }

        db = database;
        updateInterval = interval;
    }


    public Set<String> getListOfPath(Set<String> paths, String dirPath) {

        File currentDir = new File(dirPath);
        if (!currentDir.isDirectory()) {
            throw new IllegalArgumentException("wrong dir path");
        }

        for (File f: currentDir.listFiles()) {
            if (f.isFile()) {
                paths.add(f.getPath());
            }

            if (f.isDirectory()) {
                paths.addAll(getListOfPath(new HashSet<String>(), f.getPath())) ;
            }
        }
        return paths;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(updateInterval * 1000);
            } catch (InterruptedException e) {
                break;
            }

            Set<String> paths = getListOfPath(new HashSet<String>(), sharedFolder.getPath()) ;
            List<String> currentDb = db.data();

            if (currentDb.size() != paths.size()) {
                db.update(paths.toArray(new String[paths.size()]));
                continue;
            }

            for (String path : currentDb) {
                if (!paths.contains(path)) {
                    db.update(paths.toArray(new String[paths.size()]));
                    break;
                }
            }

        }
    }
}
