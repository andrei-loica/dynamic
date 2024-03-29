package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import andrei.dynamic.server.jaxb.XmlFileSettings;
import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.common.Log;
import andrei.dynamic.common.ShutdownListener;
import andrei.dynamic.common.ShutdownTask;
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

    private final DirectoryManager dir;
    private final XmlServerConfiguration initialConfig;
    private CoreManager manager;
    private Thread shutdownHook;

    public Main(final String configFilePath) throws Exception {

	JAXBContext context = JAXBContext.newInstance(
		XmlServerConfiguration.class);
	Unmarshaller um = context.createUnmarshaller();
	initialConfig = (XmlServerConfiguration) um.unmarshal(new File(
		configFilePath));

	validateConfig();

	if (initialConfig.getLogLocation() == null || initialConfig.
		getLogLocation().isEmpty()) {
	    initialConfig.setLogLevel("OFF");
	    Log.setLevel(Level.OFF);
	} else {
	    if (initialConfig.getLogLevel() == null || initialConfig.
		    getLogLevel().isEmpty()) {
		initialConfig.setLogLevel("INFO");
	    }

	    if (initialConfig.getLogLevel().toUpperCase().equals("OFF")) {
		initialConfig.setLogLevel("OFF");
		Log.setLevel(Level.OFF);
	    } else if (initialConfig.getLogLocation().toUpperCase().
		    equals("CONSOLE")) {
		Log.setStdOutput();
		Log.setLevel(initialConfig.getLogLevel());
	    } else {
		try {
		    Log.setFile(initialConfig.getLogLocation(), initialConfig.
			    isLogAppend());
		} catch (Exception ex) {
		    throw new Exception("failed setting log file: " + ex.
			    getClass()
			    + " " + ex.getMessage());
		}
		Log.setLevel(initialConfig.getLogLevel());
	    }
	}

	final XmlFileSettings fileSettings = initialConfig.getFileSettings();
	dir = new DirectoryManager(fileSettings.getRootDirectory(),
		fileSettings.getCheckPeriodMillis(), fileSettings.
		getMaxDirectoryDepth());

    }

    public static void main(final String[] args) {
	if ((args == null)) {
	    System.err.println(
		    "incorrect number of parameters; should give the server configuration file path");
	    System.exit(1);
	} else if (args.length == 0) {
	    System.err.println(
		    "incorrect number of parameters; should give the server configuration file path");
	    System.exit(1);
	} else if (args.length > 1) {
	    System.err.println(
		    "incorrect number of parameters; should give only the server configuration file path");
	    System.exit(1);
	}

	//start server
	final Main main;

	try {
	    main = new Main(args[0]);
	} catch (JAXBException ex) {
	    System.err.println(
		    "Initialization exception: could not load server configuration from given file path");
	    //ex.printStackTrace(System.err);
	    return;
	} catch (Exception ex) {
	    System.err.println("Initialization exception: " + ex.getMessage());
	    //ex.printStackTrace(System.err);
	    return;
	}

	try {
	    main.start(args[0]);
	} catch (Exception ex) {
	    System.err.println("Failed starting the server: " + ex.getMessage());
	    //ex.printStackTrace(System.err);
	}
    }

    public void start(final String configFilePath) throws Exception {

	Log.info("Starting DynamicConfig Server implementation...");
	final Thread mainThread = Thread.currentThread();
	shutdownHook = new Thread(new ShutdownTask(this, mainThread));
	Runtime.getRuntime().addShutdownHook(shutdownHook);

	Log.info("initialized shutdown hook");

	manager = new CoreManager(new ServerConfiguration(initialConfig),
		configFilePath, dir);

	try {
	    manager.start();
	} catch (Exception ex) {
	    Runtime.getRuntime().removeShutdownHook(shutdownHook);
	    Log.fatal("failed starting the server manager", ex);
	    throw ex;
	}
	Log.info("sucessfully started the server manager");

    }

    @Override
    public void onShutdown() {
	Log.info("shutdown signal");

	if (manager != null) {
	    Log.info("stopping server manager");
	    manager.stop();
	}
	try {
	    Thread.sleep(7000);
	} catch (InterruptedException ex) {
	    Log.info("interrupted");
	}
	Log.info("finished shutdown task");
	Log.close();
    }

    private void validateConfig() throws Exception {
	if (initialConfig == null) {
	    throw new Exception("could not extract server configuration");
	}

	if (initialConfig.getLocalControlPort() < 1 || initialConfig.
		getLocalControlPort() > 65535) {
	    throw new Exception("invalid localControlPort parameter value");
	}

	if (initialConfig.getLocalHttpPort() < 1 || initialConfig.
		getLocalHttpPort() > 65535) {
	    throw new Exception("invalid localHttpPort parameter value");
	}

	if (initialConfig.getLocalControlPort() == initialConfig.
		getLocalHttpPort()) {
	    throw new Exception("control and http port values must be different");
	}

	if (initialConfig.getMaxClientConnections() < 0) {
	    throw new Exception("maxClientConnections value cannot be negative");
	}

	if (initialConfig.getFileSettings() == null) {
	    throw new Exception(
		    "could not extract fileSettings from server configuration");
	}

	if (initialConfig.getFileSettings().getRootDirectory() == null
		|| initialConfig.getFileSettings().getRootDirectory().isEmpty()) {
	    throw new Exception(
		    "invalid rootDirectory attribute in file settings");
	}

	if (initialConfig.getFileSettings().getCheckPeriodMillis() < 0) {
	    throw new Exception(
		    "invalid checkPeriodMillis attribute value in file settings");
	}

	if (initialConfig.getFileSettings().getMaxDirectoryDepth() < 1) {
	    throw new Exception(
		    "invalid maxDirectoryDepth attribute value in file settings");
	}
    }

}
