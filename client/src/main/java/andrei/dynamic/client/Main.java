package andrei.dynamic.client;

import andrei.dynamic.common.Address;
import andrei.dynamic.common.ShutdownTask;
import andrei.dynamic.common.ShutdownListener;
import andrei.dynamic.common.DirectoryManager;
import java.io.File;
import java.util.logging.Level;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

/**
 *
 * @author Andrei
 */
public class Main
	implements ShutdownListener {

    private final ClientConfiguration initialConfig;
    private final Address controlAddress;
    private final Address dataAddress;
    private final DirectoryManager dir;
    private final boolean keepAlive;
    private Level logLevel;
    private ClientConnection client;
    private Thread shutdownHook;

    public Main(final String configFilePath) throws Exception {

	JAXBContext context = JAXBContext.newInstance(ClientConfiguration.class);
	Unmarshaller um = context.createUnmarshaller();
	initialConfig = (ClientConfiguration) um.unmarshal(new File(
		configFilePath));

	validateConfig();

	dir = new DirectoryManager(initialConfig.getDirectoryPath(), 0, 0);
	controlAddress = new Address(initialConfig.getRemoteControlAddress(),
		initialConfig.getRemoteControlPort());
	dataAddress = new Address(initialConfig.getRemoteDataAddress(),
		initialConfig.getRemoteDataPort());
	keepAlive = initialConfig.getKeepAlive();

	logLevel = null;
    }

    public static void main(final String[] args) {
	if ((args == null)) { //TODO vezi daca faci logger
	    System.err.println(
		    "incorrect number of parameters; should give the client configuration file path");
	    System.exit(1);
	} else if (args.length == 0) {
	    System.err.println(
		    "incorrect number of parameters; should give the client configuration file path");
	    System.exit(1);
	} else if (args.length > 1) {
	    System.err.println(
		    "incorrect number of parameters; should give only the client configuration file path");
	    System.exit(1);
	}

	//start client
	final Main main;

	try {
	    main = new Main(args[0]);
	} catch (JAXBException ex) {
	    //TODO
	    System.err.println(
		    "Initialization exception: could not load client configuration from given file path");
	    ex.printStackTrace(System.err);
	    return;
	} catch (Exception ex) {
	    //TODO
	    System.err.println("Initialization exception: " + ex.getMessage());
	    //ex.printStackTrace(System.err);
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

	client = new ClientConnection(controlAddress, dataAddress, dir,
		initialConfig.getClientAuthToken(), initialConfig.getKey());

	try {
	    client.start(keepAlive);
	} catch (Exception ex) {
	    Runtime.getRuntime().removeShutdownHook(shutdownHook);
	    throw ex;
	    //TODO
	}

    }

    private void validateConfig() throws Exception {

	if (initialConfig == null) {
	    throw new Exception("failed to extract client configuration");
	}

	if (initialConfig.getRemoteControlAddress() == null || (!initialConfig.
		getRemoteControlAddress().matches(
			"\\A(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\."
			+ "(25[0-5]|2[0-4][0-9]|[01]?[0-9]{0,2})\\z")
		&& !initialConfig.getRemoteControlAddress().matches(
			"\\Alocalhost\\z"))) {

	    throw new Exception("invalid host address in client configuration");
	}

	if (initialConfig.getRemoteControlPort() < 1 || initialConfig.
		getRemoteControlPort() > 65535) {
	    throw new Exception(
		    "invalid remote control port number in client configuration");
	}

	if (initialConfig.getRemoteDataPort() < 1 || initialConfig.
		getRemoteDataPort() > 65535) {
	    throw new Exception(
		    "invalid remote data port number in client configuration");
	}
	
	if (initialConfig.getKey() == null || initialConfig.getKey().isEmpty()){
	    throw new Exception("invalid key");
	}

	if (initialConfig.getDirectoryPath() == null) {
	    throw new Exception(
		    "could not extract directory path from client configuration");
	}

	if (initialConfig.getClientAuthToken() == null) {
	    throw new Exception(
		    "could not extract authentication token from client configuration");
	}
    }

    @Override
    public void onShutdown() {
	System.out.println("shutdown signal");
	if (client != null) {
	    client.stop();
	}
	try {
	    Thread.sleep(7000);
	} catch (InterruptedException ex) {
	    System.out.println("interrupted");
	}
	//TODO log
    }

}
