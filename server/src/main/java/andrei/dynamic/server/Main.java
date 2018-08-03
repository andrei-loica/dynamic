package andrei.dynamic.server;

import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.common.ShutdownListener;
import andrei.dynamic.common.ShutdownTask;
import java.util.logging.Level;

/**
 *
 * @author Andrei
 */
public class Main
	implements ShutdownListener {

    private final DirectoryManager dir;
    private Level logLevel;
    private ConnectionManager manager;
    private Thread shutdownHook;

    public Main(final String[] args) throws Exception {

	dir = new DirectoryManager(args[0]);
	logLevel = null;

	if (args.length == 2) {
	    switch (args[1]) {
		case "FINE":
		    if (logLevel != null) {
			throw new IllegalArgumentException(
				"logging level present more than once");
		    }
		    logLevel = Level.FINE;
		    break;
		case "INFO":
		    if (logLevel != null) {
			throw new IllegalArgumentException(
				"logging level present more than once");
		    }
		    logLevel = Level.INFO;
		    break;
		case "WARNING":
		    if (logLevel != null) {
			throw new IllegalArgumentException(
				"logging level present more than once");
		    }
		    logLevel = Level.WARNING;
		    break;
		default:
		    throw new IllegalArgumentException("unknown argument "
			    + args[1]);
	    }
	}

    }

    public static void main(final String[] args) {
	if ((args == null) || (args.length < 1)) { //TODO vezi daca faci logger
	    System.err.println(
		    "incorrect number of parameters; should have at least 1");
	    System.exit(1);
	}

	//start client
	final Main main;

	try {
	    main = new Main(args);
	} catch (Exception ex) {
	    //TODO
	    System.err.println("Initialization exception: " + ex.getMessage());
	    return;
	}

	try {
	    main.start();
	} catch (Exception ex) {
	    System.err.println("Failed starting the client: " + ex.getMessage());
	}
    }

    public void start() throws Exception {

	final Thread mainThread = Thread.currentThread();
	shutdownHook = new Thread(new ShutdownTask(this, mainThread));
	Runtime.getRuntime().addShutdownHook(shutdownHook);

	manager = new ConnectionManager(dir);

	try {
	    manager.start();
	} catch (Exception ex) {
	    Runtime.getRuntime().removeShutdownHook(shutdownHook);
	    throw ex;
	    //TODO
	}

    }

    @Override
    public void onShutdown() {
	System.out.println("shutdown signal");
	manager.stop();
	//TODO log
    }

}
