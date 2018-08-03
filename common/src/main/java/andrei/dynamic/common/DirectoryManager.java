package andrei.dynamic.common;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author Andrei
 */
public class DirectoryManager {

    private static final int MAX_DEPTH = 5;
    private static final int CHECK_PERIOD = 5000;

    private final String path;
    private final File root;
    private final ArrayList<DirectoryChangesListener> listeners;
    private DirectoryInstance lastImage;
    private Worker worker;
    private boolean working;

    public DirectoryManager(final String dirPath) throws Exception {

	path = dirPath;
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

	listeners = new ArrayList<>();
	lastImage = new DirectoryInstance(root, MAX_DEPTH);
    }

    public void registerListener(DirectoryChangesListener listener) {

	synchronized (listeners) {
	    listeners.add(listener);
	}

	if (!working) {
	    worker = new Worker(CHECK_PERIOD, this);
	    worker.start();
	    working = true;
	}

    }

    public String getPath() {
	return path;
    }

    public String getAbsolutePath() {
	return root.getAbsolutePath();
    }

    protected void refreshContentImage() throws Exception {
	final DirectoryInstance newImage
		= new DirectoryInstance(root, MAX_DEPTH); //le si sorteaza

	final LinkedList<AbstractContentNode> deleted = new LinkedList<>();
	final LinkedList<AbstractContentNode> created = new LinkedList<>();
	final LinkedList<AbstractContentNode> modified = new LinkedList<>();

	final ArrayList<AbstractContentNode> lastContent = lastImage.
		getAllFiles(MAX_DEPTH);
	final ArrayList<AbstractContentNode> newContent = newImage.getAllFiles(
		MAX_DEPTH);

	int index = 0;
	//TODO am o problema la fisierele sterse cand e un singur fisier
	//TODO problema mai mare in algoritm
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

    public void workerStopped() {

    }

    private void notifyDeleted(final AbstractContentNode file) {
	for (DirectoryChangesListener listener : listeners) {
	    try {
		listener.onFileDeleted(file);
	    } catch (Exception ex) {
		//asta n-ar trebui sa se intample never ever
	    }
	}
    }

    private void notifyCreated(final AbstractContentNode file) {
	for (DirectoryChangesListener listener : listeners) {
	    try {
		listener.onFileCreated(file);
	    } catch (Exception ex) {
		//asta n-ar trebui sa se intample never ever
	    }
	}
    }

    private void notifyModified(final AbstractContentNode file) {
	for (DirectoryChangesListener listener : listeners) {
	    try {
		listener.onFileModified(file);
	    } catch (Exception ex) {
		//asta n-ar trebui sa se intample never ever
	    }
	}
    }

    private static class Worker
	    extends Thread {

	private final DirectoryManager parent;
	private boolean keepWorking;
	private final int checkPeriod;

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

	public void stopWorking() {
	    keepWorking = false;
	}

    }

}
