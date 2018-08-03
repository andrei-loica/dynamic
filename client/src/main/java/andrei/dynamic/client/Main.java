package andrei.dynamic.client;

import andrei.dynamic.common.ShutdownTask;
import andrei.dynamic.common.ShutdownListener;
import andrei.dynamic.common.DirectoryManager;
import java.util.logging.Level;

/**
 *
 * @author Andrei
 */
public class Main
	implements ShutdownListener {

    private final Address address;
    private final DirectoryManager dir;
    private final boolean keepAlive;
    private Level logLevel;
    private ClientConnection client;
    private Thread shutdownHook;

    public Main(final String[] args) throws Exception {

	address = Address.parseAddress(args[0]);
	dir = new DirectoryManager(args[1]);
	logLevel = null;

	boolean keepAlivePresent = false;

	//parse optional args TODO vezi forul
	for (int i = 2; i < args.length; i++) {
	    switch (args[i]) {
		//keep alive
		case "keep_alive":
		    if (keepAlivePresent) {
			throw new IllegalArgumentException(
				"keep_alive argument present more than once");
		    }
		    keepAlivePresent = true;
		    break;
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
			    + args[i]);
	    }
	}

	keepAlive = keepAlivePresent;
    }

    public static void main(final String[] args) {
	if ((args == null) || (args.length < 2)) { //TODO vezi daca faci logger
	    System.err.println(
		    "incorrect number of parameters; should have at least 2");
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

    public void start() throws Exception{

	final Thread mainThread = Thread.currentThread();
	shutdownHook = new Thread(new ShutdownTask(this, mainThread));
	Runtime.getRuntime().addShutdownHook(shutdownHook);

	client = new ClientConnection(address, dir);

	try {
	    client.start(keepAlive);
	} catch (Exception ex) {
	    Runtime.getRuntime().removeShutdownHook(shutdownHook);
	    throw ex;
	    //TODO
	}

    }

    @Override
    public void onShutdown() {
	System.out.println("shutdown signal");
	client.stop();
	//TODO log
    }

}
