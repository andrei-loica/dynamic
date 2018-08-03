package andrei.dynamic.common;

/**
 *
 * @author Andrei
 */
public class ShutdownTask
	implements Runnable {

    private final ShutdownListener parent;
    private final Thread mainThread;

    public ShutdownTask(final ShutdownListener parent, final Thread mainThread) {
	this.parent = parent;
	this.mainThread = mainThread;
    }

    @Override
    public void run() {

	parent.onShutdown();

	try {
	    mainThread.join();
	} catch (Exception ex) {
	    //TODO
	}

    }
}
