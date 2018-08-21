package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import andrei.dynamic.common.AbstractContentNode;
import andrei.dynamic.common.DirectoryChangesListener;
import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.server.http.HttpManager;
import andrei.dynamic.server.jaxb.XmlFileGroup;
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
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

/**
 *
 * @author Andrei
 */
public class CoreManager
	implements DirectoryChangesListener {

    private final DirectoryManager dir;
    private final ServerConfiguration config;
    private final String configFilePath;
    private ConnectionListener connectionListener; //should never be null
    private ServerSocket dataServer;
    private HttpManager httpManager;
    private final HashMap<String, HashSet<String>> tokensForContent; //<relativeFileName, tokens>
    private final HashMap<String, HashSet<String>> contentForToken; //<token, relativeFileNames>
    private final HashSet<String> validTokens;
    private final HashSet<String> offlineTokens;
    private final HashSet<String> blockedTokens;
    private final ArrayList<ClientWithServer> authenticatingClients;
    private final HashMap<String, ClientWorker> connectedTokensWithWorkers;
    private final ArrayList<ClientWorker> clientWorkers;

    public CoreManager(final ServerConfiguration initialConfig,
	    final String configFilePath, final DirectoryManager dir) throws
	    Exception {
	this.dir = dir;
	config = initialConfig;
	this.configFilePath = configFilePath;
	validTokens = new HashSet<>();
	offlineTokens = new HashSet<>();
	blockedTokens = new HashSet<>();
	tokensForContent = new HashMap<>();
	contentForToken = new HashMap<>();
	authenticatingClients = new ArrayList<>();
	connectedTokensWithWorkers = new HashMap<>();
	clientWorkers = new ArrayList<>();

	processFileSettings();

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
		if (resource.equals(localRelative)) {
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
		if (resource.equals(localRelative)) {
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
		if (resource.equals(localRelative)) {
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

    private synchronized void connectionListenerStopped() {
	System.out.println("connection listener stopped");
	dir.stop();

	if (clientWorkers.isEmpty()) {
	    try {
		dataServer.close();
		System.out.println("closed data transfer server");
	    } catch (Exception ex) {
		System.err.println("failed to close data server: " + ex.
			getMessage());
	    }
	}
	
	for (ClientWorker worker : clientWorkers) {
	    System.out.println("sent stop for worker " + worker.getClient().
		    getStringAddress());
	    worker.stopWorking();
	}
    }

    public void httpServerStopped() {
	System.out.println("http server stopped");
    }

    private synchronized void clientWorkerStopped(final ClientWorker worker) {
	System.out.println("client connection " + worker.getClient().
		getStringAddress()
		+ " closed");

	boolean wasConnected;
	if (!(wasConnected = (connectedTokensWithWorkers.remove(worker.
		getToken())
		!= null))) {
	    System.out.println("clientul " + worker.getToken()
		    + " nu era conectat");
	}
	if (wasConnected && validTokens.contains(worker.getToken())) {
	    offlineTokens.add(worker.getToken());
	}
	if (!clientWorkers.remove(worker)) {
	    System.err.println("worker-ul nu era inregistrat");
	}
	if (authenticatingClients.remove(worker.getClient())) {
	    System.err.println("clientul " + worker.getToken()
		    + " nu a fost autentificat");
	}

	if (!connectionListener.isActive() && clientWorkers.isEmpty()) {
	    try {
		dataServer.close();
		System.out.println("closed data transfer server");
	    } catch (Exception ex) {
		System.err.println("failed to close data server: " + ex.
			getMessage());
	    }
	}
    }

    private synchronized void incomingConnection(final Socket client) {

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

    private synchronized boolean authenticatedClient(final ClientWorker worker,
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

	offlineTokens.remove(authToken);
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

    private void writeConfig() throws Exception {
	JAXBContext context = JAXBContext.newInstance(
		XmlServerConfiguration.class);
	Marshaller m = context.createMarshaller();
	m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	m.marshal(config.toJaxb(), new File(configFilePath));
    }

    private synchronized void processFileSettings() throws Exception {

	XmlFileGroup.index = 0;
	tokensForContent.clear();
	contentForToken.clear();

	for (XmlFileGroup group : config.getFileSettings().getGroups()) {
	    group.setOrder(++XmlFileGroup.index);
	    if (group.getFiles() != null) {
		for (String file : group.getFiles()) {
		    HashSet<String> tokens = tokensForContent.get(dir.
			    normalizeRelativePath(file));
		    if (tokens == null) {
			tokens = new HashSet<>();
			tokensForContent.put(dir.normalizeRelativePath(file),
				tokens);
		    }
		    if (group.getClients() != null) {
			for (String token : group.getClients()) {
			    tokens.add(token);
			    HashSet<String> files
				    = contentForToken.get(token);
			    if (files == null) {
				files = new HashSet<>();
				contentForToken.put(token, files);
			    }
			    files.add(file);
			}
		    }
		}
	    }
	}

	validTokens.clear();
	for (HashSet set : tokensForContent.values()) {
	    validTokens.addAll(set);
	}

	offlineTokens.clear();

	for (String token : validTokens) {
	    if (!connectedTokensWithWorkers.containsKey(token)) {
		offlineTokens.add(token);
	    }
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
	    
	    System.out.println(current.getKey());
	    for (String token : current.getValue()) {
		System.out.print(token + " ");
	    }
	    System.out.println();
	}

	for (Entry<String, ClientWorker> entry : connectedTokensWithWorkers.
		entrySet()) {
	    if (!validTokens.contains(entry.getKey())) {
		entry.getValue().stopWorking();
	    }
	}
    }

    //<editor-fold desc="RUNTIME EXTERNAL API">
    public Set<ClientWithServer> getConnectedClients() {
	return connectedTokensWithWorkers.values().stream().map(worker
		-> worker.getClient()).collect(Collectors.toSet());
    }

    public Set<String> getOfflineClients() {
	return offlineTokens;
    }

    public Set<String> getBlockedClients() {
	return blockedTokens;
    }

    public synchronized void blockClient(final String token) {
	blockedTokens.add(token);

	final ClientWorker worker = connectedTokensWithWorkers.get(token);
	if (worker != null) {
	    worker.stopWorking();
	}
    }

    public synchronized void unblockClient(final String token) {
	blockedTokens.remove(token);
    }

    public void closeClient(final String token) throws Exception {
	synchronized (connectedTokensWithWorkers) {
	    final ClientWorker worker = connectedTokensWithWorkers.get(token);

	    if (worker == null) {
		throw new Exception("no client connected for token " + token);
	    }

	    System.out.println("closing client " + token + " at request");
	    worker.stopWorking();
	}
    }

    public ArrayList<XmlFileGroup> getFileGroups() {
	return config.getFileSettings().getGroups();
    }

    public void setFileGroups(final ArrayList<XmlFileGroup> groups) {
	synchronized (config) {
	    config.getFileSettings().setGroups(groups);
	}
    }

    public void saveConfig() throws Exception {
	processFileSettings();
	writeConfig();
    }

    public ServerConfiguration getConfig() {
	return config;
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

	    client.setAuthToken(authToken);
	    if (!manager.validTokens.contains(authToken)) {
		System.err.println("authentication failed for client " + client.
			getStringAddress() + " with token " + authToken);
		stopWorking();
		tasks = null;
	    } else if (manager.blockedTokens.contains(authToken)) {
		System.out.println("blocked client " + client.getStringAddress()
			+ " with token " + authToken);
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
	    client.onClosing();
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

		case GLOBAL_CHECK: {

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
	    return Objects.equals(this.client, other.client);
	}

    }

}
