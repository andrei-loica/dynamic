package andrei.dynamic.server;

import andrei.dynamic.common.AbstractContentNode;
import andrei.dynamic.common.DirectoryChangesListener;
import andrei.dynamic.common.DirectoryManager;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author Andrei
 */
public class ConnectionManager
	implements DirectoryChangesListener {

    private static final String SETTINGS_FILE = "ServerSettings.xml";
    private static final int TCP_PORT = 8008;
    private final DirectoryManager dir;
    private ServerFilesSettings fileSettings;
    private ConnectionListener connectionListener;
    private final ArrayList<ClientWithServer> clientList;
    private final ArrayList<ClientWorker> clientWorkers;

    public ConnectionManager(final DirectoryManager dir) {
	this.dir = dir;
	clientList = new ArrayList<>();
	clientWorkers = new ArrayList<>();
    }

    public void start() throws Exception {

	connectionListener = new ConnectionListener(this, TCP_PORT);
	connectionListener.start();

	/*
	JAXBContext context2 = JAXBContext.
		newInstance(ServerFilesSettings.class);
	Unmarshaller um = context2.createUnmarshaller();
	fileSettings = (ServerFilesSettings) um.unmarshal(new File(
		dir.getAbsolutePath(), SETTINGS_FILE));
	 */
	dir.registerListener(this);

    }

    public void stop() {

	connectionListener.interrupt(); //TODO: vezi ca poate da exceptie; fa altcumva

    }

    @Override
    public void onFileDeleted(AbstractContentNode file) {
	synchronized (clientList) {
	    for (ClientWithServer client : clientList) {
		client.sendTestMessage("deleted " + file.getPath());
	    }
	}
    }

    @Override
    public void onFileCreated(AbstractContentNode file) {
	synchronized (clientList) {
	    for (ClientWithServer client : clientList) {
		client.sendTestMessage("created " + file.getPath());
	    }
	}
    }

    @Override
    public void onFileModified(AbstractContentNode file) {
	synchronized (clientList) {
	    for (ClientWithServer client : clientList) {
		client.sendTestMessage("modified " + file.getPath());
	    }
	}
    }

    private void connectionListenerStopped() {

    }

    private void incomingConnection(final Socket client) {

	ClientWithServer clientWrapper = new ClientWithServer(client);

	synchronized (clientList) {
	    if (!clientList.contains(clientWrapper)) {
		clientList.add(clientWrapper);
	    }
	}

	try {
	    clientWrapper.start();
	} catch (Exception ex) {
	    ex.printStackTrace(System.out);
	}
    }

    private static class ConnectionListener
	    extends Thread {

	private final ConnectionManager parent;
	private final ServerSocket serverSocket;
	private boolean isActive;

	public ConnectionListener(final ConnectionManager parent, int port)
		throws
		Exception {
	    this.parent = parent;
	    isActive = false;
	    serverSocket = new ServerSocket(port);
	    isActive = true;
	}

	public boolean isActive() {
	    return isActive;
	}

	@Override
	public void run() {
	    while (isActive) {

		Socket clientConnection = null;
		try {
		    clientConnection = serverSocket.accept();
		} catch (Exception ex) {
		    isActive = false;
		    try {
			serverSocket.close();
		    } catch (Exception ex1) {
			//nimic
		    }
		    break;
		}

		if (clientConnection != null) {
		    parent.incomingConnection(clientConnection);
		}
	    }
	    parent.connectionListenerStopped();
	}

    }

    private static class ClientWorker
	    extends Thread {

	private final ClientWithServer client;
	private final ConnectionManager manager;

	protected ClientWorker(final ClientWithServer client,
		final ConnectionManager manager) {
	    this.client = client;
	    this.manager = manager;
	}

	@Override
	public void run() {
	    try {
		client.start();
	    } catch (Exception ex) {

	    }
	}

    }

}
