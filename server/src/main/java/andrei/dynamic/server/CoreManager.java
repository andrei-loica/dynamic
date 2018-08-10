package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.ServerConfiguration;
import andrei.dynamic.common.AbstractContentNode;
import andrei.dynamic.common.DirectoryChangesListener;
import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.server.http.HttpManager;
import andrei.dynamic.server.jaxb.FileGroup;
import andrei.dynamic.server.jaxb.FileGroupElement;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 *
 * @author Andrei
 */
public class CoreManager
	implements DirectoryChangesListener {

    private final DirectoryManager dir;
    private ServerConfiguration config;
    private final String configFilePath;
    private ConnectionListener connectionListener;
    private ServerSocket dataServer;
    private HttpManager httpManager;
    private final HashMap<String, HashSet<String>> tokensForContent; //<relativeFileName, tokens>
    private final HashSet<String> validTokens;
    private final ArrayList<ClientWithServer> authenticatingClients;
    private final HashMap<String, ClientWorker> connectedTokensWithWorkers;
    private final ArrayList<ClientWorker> clientWorkers;

    public CoreManager(final ServerConfiguration initialConfig,
	    final String configFilePath, final DirectoryManager dir) throws
	    Exception {
	this.dir = dir;
	config = initialConfig;
	this.configFilePath = configFilePath;
	tokensForContent = new HashMap<>();
	authenticatingClients = new ArrayList<>();
	connectedTokensWithWorkers = new HashMap<>();
	clientWorkers = new ArrayList<>();

	for (FileGroup group : config.getFileSettings().getGroups()) {
	    if (group.getFiles() != null) {
		for (FileGroupElement file : group.getFiles()) {
		    HashSet<String> tokens = tokensForContent.get(dir.
			    normalizeRelativePath(file.
				    getLocalPath()));
		    if (tokens == null) {
			tokens = new HashSet<>();
			tokensForContent.put(dir.normalizeRelativePath(file.
				getLocalPath()), tokens);
		    }
		    if (group.getClients() != null) {
			for (String token : group.getClients()) {
			    tokens.add(token);
			}
		    }
		}
	    }
	}

	validTokens = new HashSet();
	for (HashSet set : tokensForContent.values()) {
	    validTokens.addAll(set);
	}

	for (Entry<String, HashSet<String>> current : tokensForContent.
		entrySet()) {
	    for (Entry<String, HashSet<String>> other : tokensForContent.
		    entrySet()) {
		if (current.getKey().equals(other.getKey())) {
		    continue;
		}
		if (current.getKey().startsWith(other.getKey()) && dir.
			isDirectory(other.getKey())) {
		    current.getValue().addAll(other.getValue());
		}
	    }
	    /*
	    System.out.println(current.getKey());
	    for (String token : current.getValue()) {
		System.out.print(token + " ");
	    }
	    System.out.println();*/
	}
    }

    public void start() throws Exception {

	httpManager = new HttpManager(this, config.getLocalHttpPort());
	httpManager.start();

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

	httpManager.stop();
	connectionListener.stopWorking();
	//restul pe connection listener stopped
    }

    @Override
    public void onFileDeleted(AbstractContentNode file) {
	try {
	    final String localRelative = dir.relativeFilePath(file.getPath());
	    String bestMatch = "";
	    for (String resource : tokensForContent.keySet()) {
		if (resource.equals(localRelative)){
		    bestMatch = resource;
		    break;
		}
		if (dir.isDirectory(resource) && localRelative.startsWith(
			resource) && resource.length() > bestMatch.length()) {
		    bestMatch = resource;
		}
	    }
	    if (bestMatch.equals("")) {
		return;
	    }
	    for (String token : tokensForContent.get(bestMatch)) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    worker.addTask(new ClientTask(
			    ClientTaskType.SEND_DELETE_FILE, file));
		}
	    }
	} catch (Exception ex) {
	    System.out.println("failed to send delete file " + file.getPath());
	}
    }

    @Override
    public void onFileCreated(AbstractContentNode file) {
	try {
	    final String localRelative = dir.relativeFilePath(file.getPath());
	    String bestMatch = "";
	    for (String resource : tokensForContent.keySet()) {
		if (resource.equals(localRelative)){
		    bestMatch = resource;
		    break;
		}
		if (dir.isDirectory(resource) && localRelative.startsWith(
			resource) && resource.length() > bestMatch.length()) {
		    bestMatch = resource;
		}
	    }
	    if (bestMatch.equals("")) {
		return;
	    }
	    for (String token : tokensForContent.get(bestMatch)) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    worker.addTask(new ClientTask(
			    ClientTaskType.SEND_CREATE_FILE, file));
		}
	    }
	} catch (Exception ex) {
	    System.out.println("failed to send create file " + file.getPath());
	}
    }

    @Override
    public void onFileModified(AbstractContentNode file) {
	try {
	    final String localRelative = dir.relativeFilePath(file.getPath());
	    String bestMatch = "";
	    for (String resource : tokensForContent.keySet()) {
		if (resource.equals(localRelative)){
		    bestMatch = resource;
		    break;
		}
		if (dir.isDirectory(resource) && localRelative.startsWith(
			resource) && resource.length() > bestMatch.length()) {
		    bestMatch = resource;
		}
	    }
	    if (bestMatch.equals("")) {
		return;
	    }
	    for (String token : tokensForContent.get(bestMatch)) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    worker.addTask(new ClientTask(
			    ClientTaskType.SEND_MODIFY_FILE, file));
		}
	    }
	} catch (Exception ex) {
	    System.out.println("failed to send modify file " + file.getPath());
	}
    }

    private void connectionListenerStopped() {
	System.out.println("connection listener stopped");
	dir.stop();

	for (ClientWorker worker : clientWorkers) {
	    System.out.println("sent stop for worker " + worker.getClient().
		    getStringAddress());
	    worker.stopWorking();
	}
    }

    public void httpServerStopped() {
	System.out.println("http server stopped");
    }

    private void clientWorkerStopped(final ClientWorker worker) {
	System.out.println("client connection " + worker.getClient().
		getStringAddress()
		+ " closed");

	boolean removalResult = connectedTokensWithWorkers.remove(worker.
		getToken()) != null;
	removalResult = clientWorkers.remove(worker) && removalResult;
	if (!removalResult) {
	    System.err.println("s-a intamplat ceva foarte ciudat cu worker-ul");
	}

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

	synchronized (authenticatingClients) {
	    if (!authenticatingClients.contains(clientWrapper)) {
		if (authenticatingClients.size() + connectedTokensWithWorkers.
			size() < config.getMaxClientConnections()) {
		    authenticatingClients.add(clientWrapper);
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

    private boolean authenticatedClient(final ClientWorker worker,
	    final String authToken) {

	if (!authenticatingClients.remove(worker.getClient())) {
	    System.err.println(
		    "s-a autentificat un client care nu era in curs de autentificare");
	}
	if (connectedTokensWithWorkers.containsKey(authToken)) {
	    System.err.println("client " + connectedTokensWithWorkers.get(
		    authToken).getClient().getStringAddress()
		    + " already connected with token " + authToken);
	    return false;
	}
	System.out.println("authenticated client " + worker.getClient().
		getStringAddress() + " with token " + authToken);

	connectedTokensWithWorkers.put(authToken, worker);
	return true;
    }

    public void stopClient(final String clientToken) {

	final ClientWorker worker = connectedTokensWithWorkers.remove(
		clientToken);

	if (worker == null) {
	    System.err.println("no client is connected with token "
		    + clientToken);
	    return;
	}

	System.out.println("stopping client " + worker.getClient().
		getStringAddress());
	worker.stopWorking();
    }

    private void saveConfig() throws Exception {
	JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
	Marshaller m = context.createMarshaller();
	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	m.marshal(config, new File(configFilePath));
    }

    //<editor-fold desc="RUNTIME EXTERNAL API">
    public Set<String> getConnectedClients() {
	return connectedTokensWithWorkers.keySet();
    }

    public int getMaxClientConnections() {
	synchronized (config) {
	    return config.getMaxClientConnections();
	}
    }

    public void setMaxClientConnections(int max) {
	synchronized (config) {
	    config.setMaxClientConnections(max);
	}

	try {
	    saveConfig();
	} catch (Exception ex) {
	    System.err.println("failed to save new configuration");
	}
    }

    //</editor-fold>
    private static class ConnectionListener
	    extends Thread {

	private final CoreManager parent;
	private final ServerSocket serverSocket;
	private boolean isActive;

	public ConnectionListener(final CoreManager parent, int port)
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

	public void stopWorking() {
	    isActive = false;
	}

    }

    private static class ClientWorker
	    extends Thread {

	public final static int WAIT_TIME = 2000;
	private ClientWithServer client;
	private CoreManager manager;
	private LinkedBlockingQueue<ClientTask> tasks;
	private String authToken;
	private boolean working;

	protected ClientWorker(final ClientWithServer client,
		final CoreManager manager) {
	    this.client = client;
	    this.manager = manager;
	    tasks = new LinkedBlockingQueue<>();
	    working = true;
	}

	@Override
	public void run() {
	    try {
		authToken = client.testConnection();
		if (authToken == null) {
		    System.err.println("failed authentication with client "
			    + client.getStringAddress());
		    stopWorking();
		    tasks = null;
		}
	    } catch (Exception ex) {
		System.err.println("exception while authenticating client "
			+ client.getStringAddress());
		stopWorking();
		tasks = null;
	    }

	    if (!manager.validTokens.contains(authToken)) {
		System.err.println("authentication failed for client " + client.
			getStringAddress() + " with token " + authToken);
		stopWorking();
		tasks = null;
	    } else if (!manager.authenticatedClient(this, authToken)) {
		stopWorking();
		tasks = null;
	    }

	    while (working) {
		try {
		    ClientTask task = tasks.poll(WAIT_TIME,
			    TimeUnit.MILLISECONDS);

		    if (task != null) {
			executeTask(task);
			continue;
		    }

		    final String token = client.testConnection();

		    if (!authToken.equals(token)) {
			if (token != null) {
			    System.err.println(
				    "received wrong token while testing connection on client "
				    + client.getStringAddress());
			}
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

	    while (tasks != null && tasks.size() > 0) {
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

	public String getToken() {
	    return authToken;
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
