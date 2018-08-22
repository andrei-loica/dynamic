package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import andrei.dynamic.server.jaxb.XmlFileSettings;
import andrei.dynamic.common.DirectoryManager;
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

    private static final int DEFAULT_CHECK_PERIOD = 3000;
    private static final int DEFAULT_MAX_DEPTH = 5;

    private final DirectoryManager dir;
    private Level logLevel;
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

	final XmlFileSettings fileSettings = initialConfig.getFileSettings();
	if (fileSettings.getCheckPeriodMillis() == 0) {
	    fileSettings.setCheckPeriodMillis(DEFAULT_CHECK_PERIOD);
	}
	if (fileSettings.getMaxDirectoryDepth() == 0) {
	    fileSettings.setMaxDirectoryDepth(DEFAULT_MAX_DEPTH);
	}

	dir = new DirectoryManager(fileSettings.getRootDirectory(),
		fileSettings.getCheckPeriodMillis(), fileSettings.
		getMaxDirectoryDepth());

	logLevel = null;
    }

    public static void main(final String[] args) {
	if ((args == null)) { //TODO vezi daca faci logger
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

	//start client
	final Main main;

	try {
	    main = new Main(args[0]);
	} catch (JAXBException ex) {
	    //TODO
	    System.err.println(
		    "Initialization exception: could not load server configuration from given file path");
	    ex.printStackTrace(System.err);
	    return;
	} catch (Exception ex) {
	    //TODO
	    System.err.println("Initialization exception: " + ex.getMessage());
	    //ex.printStackTrace(System.err);
	    return;
	}

	try {
	    main.start(args[0]);
	} catch (Exception ex) {
	    System.err.println("Failed starting the server: " + ex.getMessage());
	    ex.printStackTrace(System.err);
	}
    }

    public void start(final String configFilePath) throws Exception {

	final Thread mainThread = Thread.currentThread();
	shutdownHook = new Thread(new ShutdownTask(this, mainThread));
	Runtime.getRuntime().addShutdownHook(shutdownHook);

	manager = new CoreManager(new ServerConfiguration(initialConfig),
		configFilePath, dir);

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
	if (manager != null) {
	    manager.stop();
	}
	try {
	    Thread.sleep(7000);
	} catch (InterruptedException ex) {
	    System.out.println("interrupted");
	}
	//TODO log
    }

    private void validateConfig() throws Exception {
	if (initialConfig == null) {
	    throw new Exception("could not extract server configuration");
	}

	if (initialConfig.getLocalControlPort() < 1 || initialConfig.
		getLocalControlPort() > 65535) {
	    throw new Exception("invalid control port number");
	}

	if (initialConfig.getLocalDataPort() < 1 || initialConfig.
		getLocalDataPort() > 65535) {
	    throw new Exception("invalid data port number");
	}

	if (initialConfig.getLocalHttpPort() < 1 || initialConfig.
		getLocalHttpPort() > 65535) {
	    throw new Exception("invalid data port number");
	}

	if (initialConfig.getLocalControlPort() == initialConfig.
		getLocalDataPort()) {
	    throw new Exception("control and data port must be different");
	}

	if (initialConfig.getLocalControlPort() == initialConfig.
		getLocalHttpPort()) {
	    throw new Exception("control and http port must be different");
	}

	if (initialConfig.getLocalDataPort() == initialConfig.getLocalHttpPort()) {
	    throw new Exception("data and http port must be different");
	}
	
	if (initialConfig.getKey() == null || initialConfig.getKey().isEmpty()){
	    throw new Exception("invalid key");
	}

	if (initialConfig.getFileSettings() == null) {
	    throw new Exception(
		    "could not extract file settings from server configuration");
	}

	if (initialConfig.getFileSettings().getGroups() == null) {
	    throw new Exception(
		    "could not extract file groups from server configuration");
	}

	if (initialConfig.getFileSettings().getRootDirectory() == null
		|| initialConfig.getFileSettings().getRootDirectory().isEmpty()) {
	    throw new Exception("invalid root directory in server configuration");
	}

	if (initialConfig.getFileSettings().getCheckPeriodMillis() < 0) {
	    throw new Exception("invalid check period in server configuration");
	}

	if (initialConfig.getFileSettings().getMaxDirectoryDepth() < 0) {
	    throw new Exception(
		    "invalid maximum directory depth in server configuration");
	}
    }

}
