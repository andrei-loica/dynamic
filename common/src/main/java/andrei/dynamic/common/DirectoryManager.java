package andrei.dynamic.common;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Andrei
 */
public final class DirectoryManager {

    private int checkPeriod;
    private int maxDepth;
    private final File root;
    private DirectoryChangesListener listener;
    private DirectoryInstance lastImage;
    private final HashMap<String, byte[]> checkSums;
    private Worker worker;
    private State state;
    private final Object lock;

    public final DirectoryPathHelper paths;

    public DirectoryManager(final String dirPath, int checkPeriod, int maxDepth)
	    throws Exception {

	this.checkPeriod = checkPeriod;
	this.maxDepth = maxDepth;
	state = State.NOT_INITIALIZED;
	lock = new Object();

	try {
	    root = new File(dirPath);
	} catch (Exception ex) {
	    throw new Exception("invalid directory path");
	}
	if (!root.isDirectory()) {
	    throw new Exception(
		    "given directory path does not point to a directory");
	}

	paths = new DirectoryPathHelper(root.getAbsolutePath());
	listener = null;
	checkSums = new HashMap<>();
	lastImage = new DirectoryInstance(root, maxDepth, null);

	state = State.INITIALIZED_NOT_REGISTERED;
    }

    public synchronized void registerListener(
	    final DirectoryChangesListener listener) throws
	    IllegalStateException {

	if (state != State.INITIALIZED_NOT_REGISTERED) {
	    throw new IllegalStateException("illegal register in state " + state);
	}

	this.listener = listener;
	state = State.REGISTERED_NOT_WORKING;
	loadFiles();
	checkWorker();

    }

    public int getCheckPeriod() {
	return checkPeriod;
    }

    public void setCheckPeriod(int checkPeriod) {
	this.checkPeriod = checkPeriod;
    }

    public int getMaxDepth() {
	return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
	synchronized (lock) {
	    this.maxDepth = maxDepth;
	}
    }

    public boolean isWorking() {
	return state == State.REGISTERED_WORKING;
    }

    public String getAbsolutePath() {
	return paths.root();
    }

    public synchronized void checkWorker() {

	if (state == State.INITIALIZED_NOT_REGISTERED || state
		== State.NOT_INITIALIZED || state
		== State.DEREGISTERED_WAITING_WORKER_STOP) {
	    return;
	}

	if (state == State.REGISTERED_NOT_WORKING && checkPeriod > 0) {
	    worker = new Worker(checkPeriod, this);
	    worker.start();
	    state = State.REGISTERED_WORKING;
	}

	if (state == State.REGISTERED_WORKING) {
	    if (checkPeriod < 1) {
		state = State.REGISTERED_NOT_WORKING;
		worker.stopWorking();
		worker = null;
	    } else {
		if (worker != null) {
		    worker.setCheckPeriod(checkPeriod);
		} else {
		    Log.fatal("directory manager worker is null in state "
			    + state);
		}
	    }
	}

    }

    public boolean isDirectory(final String relative) {
	return Files.isDirectory(FileSystems.getDefault().getPath(
		getAbsolutePath(), relative));
    }

    protected void refreshContentImage() throws Exception {

	final LinkedList<FileInstance> deleted = new LinkedList<>();
	final LinkedList<FileInstance> created = new LinkedList<>();
	final LinkedList<FileInstance> modified = new LinkedList<>();

	synchronized (lock) {
	    final DirectoryInstance newImage
		    = new DirectoryInstance(root, maxDepth, null); //le si sorteaza
	    final ArrayList<FileInstance> lastContent;
	    final ArrayList<FileInstance> newContent;
	    lastContent = lastImage.getAllFiles(maxDepth);
	    newContent = newImage.getAllFiles(maxDepth);

	    int index = 0;

	    for (FileInstance oldFile : lastContent) {
		if (oldFile == null) {
		    continue;
		}
		if (index >= newContent.size()) {
		    deleted.add(oldFile);
		    checkSums.remove(paths.relativeFilePath(oldFile.getPath()));
		    continue;
		}
		FileInstance newFile = newContent.get(index);
		if (newFile == null) {
		    index++;
		    continue;
		}

		int compResult = oldFile.getPath().compareTo(newFile.getPath());
		if (compResult == 0) {
		    if (oldFile.getLastModifiedDate() != newFile.
			    getLastModifiedDate()) {
			checkSums.put(newFile.getPath(), getLocalFileMD5(
				newFile.
					getPath()));
			modified.add(newFile);
		    }
		    index++;
		} else if (compResult < 0) {
		    checkSums.remove(paths.relativeFilePath(oldFile.getPath()));
		    deleted.add(oldFile);
		} else {
		    while (compResult > 0) {
			checkSums.put(newFile.getPath(), getLocalFileMD5(
				newFile.
					getPath()));
			created.add(newFile);
			if (++index < newContent.size()) {
			    newFile = newContent.get(index);
			    compResult = oldFile.getPath().compareTo(newFile.
				    getPath());
			} else {
			    break;
			}
		    }
		    if (index >= newContent.size()) {
			// nimic
		    } else if (compResult != 0) {
			deleted.add(oldFile);
			checkSums.remove(paths.relativeFilePath(oldFile.
				getPath()));
		    } else {
			if (oldFile.getLastModifiedDate() != newFile.
				getLastModifiedDate()) {
			    checkSums.put(newFile.getPath(), getLocalFileMD5(
				    newFile.getPath()));
			    modified.add(newFile);
			}
			index++;
		    }
		}
	    }

	    while (index < newContent.size()) {
		final FileInstance file = newContent.get(index++);
		checkSums.put(file.getPath(), getLocalFileMD5(file.getPath()));
		created.add(file);
	    }
	    lastImage = newImage;
	}

	for (FileInstance file : deleted) {
	    notifyDeleted(file);
	}

	for (FileInstance file : created) {
	    notifyCreated(file);
	}

	for (FileInstance file : modified) {
	    notifyModified(file);
	}
    }

    public synchronized void deregisterListener() {

	state = State.DEREGISTERED_WAITING_WORKER_STOP;
	if (worker != null) {
	    worker.stopWorking();
	}

    }

    public byte[] getCheckSum(final String absolute) {
	synchronized (lock) {
	    return checkSums.get(absolute);
	}
    }

    @SuppressWarnings("empty-statement")
    public byte[] getLocalFileMD5(final String absolute) throws Exception {

	return paths.getLocalFileMD5(absolute);
    }

    public void loadFiles() {

	if (state != State.REGISTERED_NOT_WORKING && state
		!= State.REGISTERED_WORKING) {
	    throw new IllegalStateException("illegal loadFiles in state "
		    + state);
	}
	ArrayList<FileInstance> files;
	synchronized (lock) {
	    files = lastImage.getAllFiles(maxDepth);

	    if (Log.isDebugEnabled()) {
		Log.debug("loading " + files.size()
			+ " files from root directory");
	    }
	    for (FileInstance file : files) {
		try {
		    checkSums.put(file.getPath(),
			    getLocalFileMD5(file.getPath()));
		    notifyLoaded(file);
		} catch (Exception ex) {
		    Log.debug("failed to load file " + file);
		}
	    }
	}

    }

    private synchronized void workerStopped() {
	worker = null;
	switch (state) {
	    case DEREGISTERED_WAITING_WORKER_STOP:
		listener = null;
		state = State.INITIALIZED_NOT_REGISTERED;
		break;

	    case REGISTERED_WORKING:
		state = State.REGISTERED_NOT_WORKING;
		break;
	}
    }

    private void notifyLoaded(final FileInstance file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileLoaded(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private void notifyCreated(final FileInstance file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileCreated(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private void notifyModified(final FileInstance file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileModified(file);
	} catch (Exception ex) {
	    //asta n-ar trebui sa se intample never ever
	}
	//}
    }

    private void notifyDeleted(final FileInstance file) {
	//for (DirectoryChangesListener listener : listeners) {
	try {
	    listener.onFileDeleted(file);
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

	public boolean isWorking() {
	    return keepWorking;
	}

    }

    public static enum State {
	NOT_INITIALIZED,
	INITIALIZED_NOT_REGISTERED,
	REGISTERED_NOT_WORKING,
	REGISTERED_WORKING,
	DEREGISTERED_WAITING_WORKER_STOP,
    }

}
