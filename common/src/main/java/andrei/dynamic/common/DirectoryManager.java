package andrei.dynamic.common;

import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Andrei
 */
public class DirectoryManager {

    private static final String TEMP_DIR = "temp";
    private final String path;
    private int checkPeriod;
    private int maxDepth;
    private final File root;
    private DirectoryChangesListener listener;
    private DirectoryInstance lastImage;
    private Worker worker;
    private boolean working;

    public DirectoryManager(final String dirPath, int checkPeriod, int maxDepth)
	    throws Exception {

	path = dirPath;
	this.checkPeriod = checkPeriod;
	this.maxDepth = maxDepth;
	working = false;

	try {
	    root = new File(dirPath);
	} catch (Exception ex) {
	    throw new Exception("invalid directory path");
	}
	if (!root.isDirectory()) {
	    throw new Exception(
		    "given directory path does not point to a directory");
	}

	listener = null;
	lastImage = new DirectoryInstance(root, maxDepth);
    }

    public void registerListener(final DirectoryChangesListener listener) {

	this.listener = listener;

	if (!working) {
	    worker = new Worker(checkPeriod, this);
	    worker.start();
	    working = true;
	}

    }

    public String getPath() {
	return path;
    }

    public int getCheckPeriod() {
	return checkPeriod;
    }

    public void setCheckPeriod(int checkPeriod) {
	this.checkPeriod = checkPeriod;
	worker.setCheckPeriod(checkPeriod);
    }

    public boolean isWorking() {
	return working;
    }

    public String getAbsolutePath() {
	return root.getAbsolutePath();
    }

    public String relativeFilePath(final String absolute) throws Exception {
	if (!absolute.startsWith(getAbsolutePath())) {
	    throw new PathMatchException();
	}

	return absolute.substring(getAbsolutePath().length(), absolute.length());
    }

    protected void refreshContentImage() throws Exception {
	final DirectoryInstance newImage
		= new DirectoryInstance(root, maxDepth); //le si sorteaza

	final LinkedList<AbstractContentNode> deleted = new LinkedList<>();
	final LinkedList<AbstractContentNode> created = new LinkedList<>();
	final LinkedList<AbstractContentNode> modified = new LinkedList<>();

	final ArrayList<AbstractContentNode> lastContent = lastImage.
		getAllFiles(maxDepth);
	final ArrayList<AbstractContentNode> newContent = newImage.getAllFiles(
		maxDepth);

	int index = 0;

	for (AbstractContentNode oldFile : lastContent) {
	    if (oldFile == null) {
		continue;
	    }
	    if (index >= newContent.size()) {
		deleted.add(oldFile);
		continue;
	    }
	    AbstractContentNode newFile = newContent.get(index);
	    if (newFile == null) {
		index++;
		continue;
	    }

	    int compResult = oldFile.getPath().compareTo(newFile.getPath());
	    if (compResult == 0) {
		if (oldFile.getLastModifiedDate() != newFile.
			getLastModifiedDate()) {
		    modified.add(newFile);
		}
		index++;
	    } else if (compResult < 0) {
		deleted.add(oldFile);
	    } else {
		while (compResult > 0) {
		    created.add(newFile);
		    if (index < newContent.size() - 1) {
			newFile = newContent.get(++index);
			compResult = oldFile.getPath().compareTo(newFile.
				getPath());
		    } else {
			break;
		    }
		}
		if (compResult != 0) {
		    deleted.add(oldFile);
		} else {
		    if (oldFile.getLastModifiedDate() != newFile.
			    getLastModifiedDate()) {
			modified.add(newFile);
		    }
		    index++;
		}
	    }
	}

	if (index < newContent.size()) {
	    created.addAll(newContent.subList(index, newContent.size()));
	}

	for (AbstractContentNode file : deleted) {
	    notifyDeleted(file);
	}

	for (AbstractContentNode file : created) {
	    notifyCreated(file);
	}

	for (AbstractContentNode file : modified) {
	    notifyModified(file);
	}

	lastImage = newImage;
    }

    public void stop() {

	working = false;
	if (worker != null) {
	    worker.stopWorking();
	}

    }

    public Path getTempFilePath(final String relativePath) throws Exception {
	Path absolute = FileSystems.getDefault().getPath(root.getAbsolutePath(),
		TEMP_DIR, relativePath.trim());
	Files.createDirectories(absolute.getParent());
	try {
	    (new File(absolute.toString())).createNewFile();
	} catch (FileAlreadyExistsException ex) {
	    //nimic
	}

	return absolute;
    }

    private void workerStopped() {
	if (working) {
	    worker = new Worker(checkPeriod, this);
	    worker.start();
	} else {
	    listener = null;
	}
    }

    private void notifyDeleted(final AbstractContentNode file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileDeleted(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private void notifyCreated(final AbstractContentNode file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileCreated(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private void notifyModified(final AbstractContentNode file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileModified(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private static class Worker
	    extends Thread {

	private final DirectoryManager parent;
	private boolean keepWorking;
	private int checkPeriod;

	protected Worker(int checkPeriod, final DirectoryManager directory) {
	    parent = directory;
	    keepWorking = true;
	    this.checkPeriod = checkPeriod;
	}

	@Override
	public void run() {

	    while (keepWorking) {

		try {
		    Thread.sleep(checkPeriod);
		} catch (Exception ex) {
		    keepWorking = false;
		    break;
		}

		try {
		    parent.refreshContentImage(); //refresh + trimitere modificari la listeneri
		} catch (Exception ex) {
		    //TODO: log
		}

	    }

	    parent.workerStopped();

	}

	public int getCheckPeriod() {
	    return checkPeriod;
	}

	public void setCheckPeriod(int checkPeriod) {
	    this.checkPeriod = checkPeriod;
	}

	public void stopWorking() {
	    keepWorking = false;
	}

    }

}
