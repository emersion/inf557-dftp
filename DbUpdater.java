import java.io.File;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sathouel on 22/11/2017.
 */
public class DbUpdater implements Runnable, MessageHandler {

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

        if (db == null) {
            throw new InvalidParameterException("invalid argument database is null");
        }

        sharedFolder = new File(sharedFolderPath);

        if (!sharedFolder.isDirectory()) {
            throw new InvalidParameterException("path provided is not a directory");
        }

        db = database;
        updateInterval = interval;
    }


    @Override
    public void handleMessage(Envelope msg) {
        // no op
    }

    public List<String> getListOfPath(List<String> paths, String dirPath) {

        File currentDir = new File(dirPath);
        if (!currentDir.isDirectory()) {
            throw new IllegalArgumentException("wrong dir path");
        }

        for (File f: currentDir.listFiles()) {
            if (f.isFile()) {
                paths.add(f.getPath());
            }

            if (f.isDirectory()) {
                paths.addAll(getListOfPath(new ArrayList<String>(), f.getPath())) ;
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

            List<String> paths = getListOfPath(new ArrayList<String>(), sharedFolder.getPath()) ;
            String[] newData = paths.toArray(new String[paths.size()]);
            db.update(newData);

        }
    }
}
