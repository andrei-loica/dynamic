package andrei.dynamic.server;

import andrei.dynamic.common.AbstractContentNode;
import andrei.dynamic.common.DirectoryChangesListener;
import andrei.dynamic.common.DirectoryManager;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Andrei
 */
public class ConnectionManager
	implements DirectoryChangesListener {

    private final DirectoryManager dir;
    private ServerConfiguration config;
    private ConnectionListener connectionListener;
    private ServerSocket dataServer;
    private final ArrayList<ClientWithServer> clientList;
    private final ArrayList<ClientWorker> clientWorkers;

    public ConnectionManager(final ServerConfiguration initialConfig,
	    final DirectoryManager dir) {
	this.dir = dir;
	config = initialConfig;
	clientList = new ArrayList<>();
	clientWorkers = new ArrayList<>();
    }

    public void start() throws Exception {

	connectionListener = new ConnectionListener(this, config.
		getLocalControlPort());
	dataServer = new ServerSocket(config.getLocalDataPort());
	connectionListener.start();

	dir.registerListener(this);

    }

    public Socket acceptDataConnection() throws Exception {
	return dataServer.accept();
    }

    public void stop() {

	connectionListener.stopWorking();
	//restul pe connection listener stopped
    }

    @Override
    public void onFileDeleted(AbstractContentNode file) {
	try {
	    for (ClientWorker worker : clientWorkers) {
		worker.addTask(new ClientTask(
			ClientTaskType.SEND_DELETE_FILE, file));
	    }
	} catch (Exception ex) {

	}
    }

    @Override
    public void onFileCreated(AbstractContentNode file) {
	try {
	    for (ClientWorker worker : clientWorkers) {
		worker.addTask(new ClientTask(
			ClientTaskType.SEND_CREATE_FILE, file));
	    }
	} catch (Exception ex) {

	}
    }

    @Override
    public void onFileModified(AbstractContentNode file) {
	try {
	    for (ClientWorker worker : clientWorkers) {
		worker.addTask(new ClientTask(
			ClientTaskType.SEND_MODIFY_FILE, file));
	    }
	} catch (Exception ex) {

	}
    }

    private void connectionListenerStopped() {
	System.out.println("stopped connection listener");
	dir.stop();

	for (ClientWorker worker : clientWorkers) {
	    System.out.println("sent stop for worker " + worker.getClient().
		    getStringAddress());
	    worker.stopWorking();
	}
    }

    private void clientWorkerStopped(final ClientWorker worker) {
	System.out.println("client " + worker.getClient().getStringAddress()
		+ " finalized");

	System.out.println(clientList.remove(worker.getClient()));
	System.out.println(clientWorkers.remove(worker));

	if (clientWorkers.isEmpty()) {
	    try {
		dataServer.close();
	    } catch (Exception ex) {
		System.err.println("failed to close data server: " + ex.
			getMessage());
	    }
	}
    }

    private void incomingConnection(final Socket client) {

	ClientWithServer clientWrapper;
	ClientWorker clientWorker;
	try {
	    clientWrapper = new ClientWithServer(client, config.
		    getLocalDataPort(), this);
	    clientWorker = new ClientWorker(clientWrapper, this);
	} catch (Exception ex) {
	    //TODO failed to attach wrapper
	    System.out.println("failed to attach client wrapper for " + client.
		    getInetAddress());
	    return;
	}

	synchronized (clientList) {
	    if (!clientList.contains(clientWrapper)) {
		if (clientList.size() < config.getMaxClientConnections()) {
		    clientList.add(clientWrapper);
		    clientWorkers.add(clientWorker);
		    clientWorker.start();
		} else {
		    System.out.println("refused client connection");
		    try {
			client.close();
		    } catch (Exception ex) {
			System.err.println("failed closing the client");
			//TODO naspa
		    }
		}
	    }
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
	    try {
		serverSocket.setSoTimeout(4000);
	    } catch (Exception ex) {
		System.out.println(
			"failed setting socket timeout for connection listener");
	    }
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
		} catch (SocketTimeoutException ex) {
		    //nimic
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
	
	public void stopWorking(){
	    isActive = false;
	}

    }

    private static class ClientWorker
	    extends Thread {

	public final static int WAIT_TIME = 2000;
	private ClientWithServer client;
	private ConnectionManager manager;
	private LinkedBlockingQueue<ClientTask> tasks;
	private boolean working;

	protected ClientWorker(final ClientWithServer client,
		final ConnectionManager manager) {
	    this.client = client;
	    this.manager = manager;
	    tasks = new LinkedBlockingQueue<>();
	    working = true;
	}

	@Override
	public void run() {

	    while (working) {
		try {
		    ClientTask task = tasks.poll(WAIT_TIME,
			    TimeUnit.MILLISECONDS);

		    if (task != null) {
			executeTask(task);
		    } else if (!client.testConnection()) {
			stopWorking();
			client.disconnect();
			manager.clientWorkerStopped(this);
			client = null;
			manager = null;
			tasks = null;
			return;
		    }
		} catch (Exception ex) {
		    //TODO vezi ca nu s-a putut lua un task
		    System.err.println("failed processing task");
		}
	    }

	    while (tasks.size() > 0) {
		try {
		    ClientTask task = tasks.poll(WAIT_TIME,
			    TimeUnit.MILLISECONDS);

		    if (task == null) {
			break;
		    }

		    executeTask(task);
		} catch (Exception ex) {
		    System.out.println(
			    "failed processing task; remaining tasks: " + tasks.
				    size());
		    break;
		    //TODO vezi ca au mai ramas tasks.size() task-uri
		}
	    }

	    client.disconnect();
	    manager.clientWorkerStopped(this);
	    client = null;
	    manager = null;
	    tasks = null;
	}

	public void stopWorking() {
	    working = false;
	}

	public ClientWithServer getClient() {
	    return client;
	}

	public boolean addTask(final ClientTask task) {
	    if (working) {
		try {
		    tasks.offer(task, WAIT_TIME, TimeUnit.MILLISECONDS);
		} catch (Exception ex) {
		    //TODO cu naspa
		    System.err.println("failed passing task to client "
			    + client.getStringAddress());
		    return false;
		}
		return true;
	    } else {
		return false;
	    }
	}

	private void executeTask(final ClientTask task) throws Exception {
	    switch (task.type) {
		case SEND_CREATE_FILE: {
		    AbstractContentNode file
			    = (AbstractContentNode) task.object;
		    client.sendCreateFileMessage(manager.dir.relativeFilePath(
			    file.getPath()), file.getPath());
		    break;
		}

		case SEND_DELETE_FILE: {
		    AbstractContentNode file
			    = (AbstractContentNode) task.object;
		    client.sendDeleteFileMessage(manager.dir.relativeFilePath(
			    file.getPath()), file.getPath());
		    break;
		}

		case SEND_MODIFY_FILE: {
		    AbstractContentNode file
			    = (AbstractContentNode) task.object;
		    client.sendModifyFileMessage(manager.dir.
			    relativeFilePath(
				    file.getPath()), file.getPath());
		    break;
		}
	    }
	}

	@Override
	public int hashCode() {
	    int hash = 3;
	    hash = 47 * hash + Objects.hashCode(this.client);
	    return hash;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj) {
		return true;
	    }
	    if (obj == null) {
		return false;
	    }
	    if (getClass() != obj.getClass()) {
		return false;
	    }
	    final ClientWorker other = (ClientWorker) obj;
	    if (!Objects.equals(this.client, other.client)) {
		return false;
	    }
	    return true;
	}

    }

}
