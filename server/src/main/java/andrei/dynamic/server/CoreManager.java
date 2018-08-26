package andrei.dynamic.server;

import andrei.dynamic.common.Address;
import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import andrei.dynamic.common.DirectoryChangesListener;
import andrei.dynamic.common.DirectoryManager;
import andrei.dynamic.common.DirectoryPathHelper;
import andrei.dynamic.common.FileInstance;
import andrei.dynamic.common.Log;
import andrei.dynamic.common.MustResetConnectionException;
import andrei.dynamic.server.http.HttpManager;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
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
    private boolean stopped;
    private final byte[] key;
    private final byte[] iv;
    private ConnectionListener connectionListener; //should never be null
    private ServerSocket dataServer;
    private HttpManager httpManager;
    private final HashMap<String, HashSet<String>> tokensForContent; //<relativeFileName, tokens>
    private final HashMap<String, HashSet<String>> contentForToken; //<token, relativeFileNames>
    private final HashMap<String, HashSet<String>> runtimeTokensForFile;
    private final HashMap<String, HashSet<String>> runtimeFilesForToken;
    private final HashSet<String> validTokens;
    private final HashSet<String> offlineTokens;
    private final HashSet<String> blockedTokens;
    private final ArrayList<ConnectionWrapper> authenticatingClients;
    private final HashMap<String, ClientWorker> connectedTokensWithWorkers;
    private final ArrayList<ClientWorker> clientWorkers;

    public CoreManager(final ServerConfiguration initialConfig,
	    final String configFilePath, final DirectoryManager dir) throws
	    Exception {
	this.dir = dir;
	config = initialConfig;
	this.configFilePath = configFilePath;
	stopped = false;
	validTokens = new HashSet<>();
	offlineTokens = new HashSet<>();
	blockedTokens = new HashSet<>();
	tokensForContent = new HashMap<>();
	contentForToken = new HashMap<>();
	authenticatingClients = new ArrayList<>();
	connectedTokensWithWorkers = new HashMap<>();
	clientWorkers = new ArrayList<>();
	runtimeTokensForFile = new HashMap<>();
	runtimeFilesForToken = new HashMap<>();

	if (initialConfig.getKey() == null || initialConfig.getKey().isEmpty()) {
	    key = null;
	    iv = null;
	} else {
	    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    final byte[] bytes = digest.digest(initialConfig.getKey().getBytes(
		    StandardCharsets.UTF_8));
	    key = Arrays.copyOf(bytes, 16);
	    iv = Arrays.copyOfRange(bytes, 16, 32);
	}
	processFileSettings();

    }

    public void start() throws Exception {

	httpManager = new HttpManager(this, config.getLocalAddress(), config.
		getLocalHttpPort());
	httpManager.start();

	connectionListener = new ConnectionListener(this, config.
		getLocalAddress(), config.getLocalControlPort());

	dataServer = new ServerSocket();
	dataServer.bind(new InetSocketAddress(config.getLocalAddress(), config.
		getLocalDataPort()));
	try {
	    dataServer.setSoTimeout(2000);
	} catch (Exception ex) {
	    Log.warn("failed to set data transfer server timeout", ex);
	}
	connectionListener.start();

	dir.registerListener(this);

    }

    public Socket acceptDataConnection() throws Exception {
	return dataServer.accept();
    }

    public void stop() {

	stopped = true;
	httpManager.stop();
	connectionListener.stopWorking();
	//restul pe connection listener stopped
    }

    @Override
    public synchronized void onFileLoaded(FileInstance file) {
	try {
	    final String localRelative = dir.paths.relativeFilePath(file.
		    getPath());
	    String bestMatch = "";
	    for (String resource : tokensForContent.keySet()) {
		if (resource.equals(localRelative)) {
		    bestMatch = resource;
		    break;
		}

		if (((dir.isDirectory(resource) && localRelative.startsWith(
			resource + '/')) || "/".equals(resource)) && resource.
			length() > bestMatch.length()) {
		    bestMatch = resource;
		}
	    }
	    if (bestMatch.equals("")) {
		return;
	    }
	    final HashSet<String> tokens = new HashSet();
	    runtimeTokensForFile.put(file.getPath(), tokens);
	    for (String token : tokensForContent.get(bestMatch)) {

		tokens.add(token);
		HashSet<String> files = runtimeFilesForToken.get(token);
		files.add(file.getPath());

	    }
	} catch (Exception ex) {
	    Log.warn("failed to load file " + file.getPath(), ex);
	}
    }

    @Override
    @SuppressWarnings("NestedSynchronizedStatement")
    public synchronized void onFileCreated(FileInstance file) {
	try {
	    final String localRelative = dir.paths.relativeFilePath(file.
		    getPath());
	    String bestMatch = "";
	    for (String resource : tokensForContent.keySet()) {
		if (resource.equals(localRelative)) {
		    bestMatch = resource;
		    break;
		}
		if (((dir.isDirectory(resource) && localRelative.startsWith(
			resource + '/')) || "/".equals(resource)) && resource.
			length() > bestMatch.length()) {
		    bestMatch = resource;
		}
	    }
	    if (bestMatch.equals("")) {
		return;
	    }
	    final HashSet<String> tokens = new HashSet(); //override
	    runtimeTokensForFile.put(file.getPath(), tokens);
	    int count = 0;
	    for (String token : tokensForContent.get(bestMatch)) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    if (worker.addTask(new ClientTask(
			    ClientTaskType.UPDATE_REMOTE_FILE, file)) && worker.
			    addTask(new ClientTask(
				    ClientTaskType.CHECK_FILE, file.getPath()))) {
			count++;
		    } else if (Log.isTraceEnabled()) {
			Log.debug("could not add update file " + file.
				getPath() + " for token " + token);
		    }
		}
		tokens.add(token);
		HashSet<String> files = runtimeFilesForToken.get(token);
		synchronized (files) {
		    files.add(file.getPath());
		}

	    }
	    if (Log.isDebugEnabled()) {
		Log.debug("sending update on " + count + " clients for file "
			+ file.getPath());
	    }
	} catch (Exception ex) {
	    Log.warn("failed to send update for file " + file.getPath(), ex);
	}
    }

    @Override
    public synchronized void onFileModified(FileInstance file) {
	try {
	    HashSet<String> tokens = runtimeTokensForFile.get(file.getPath());
	    if (tokens == null || tokens.isEmpty()) {
		Log.debug("modified file " + file.getPath()
			+ " but no clients are listening");
		return;
	    }
	    int count = 0;
	    for (String token : tokens) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    if (worker.addTask(new ClientTask(
			    ClientTaskType.UPDATE_REMOTE_FILE, file)) && worker.
			    addTask(new ClientTask(
				    ClientTaskType.CHECK_FILE, file.getPath()))) {
			count++;
		    } else if (Log.isTraceEnabled()) {
			Log.trace("could not add update file " + file.
				getPath() + " for token " + token);
		    }
		}
	    }
	    if (Log.isDebugEnabled()) {
		Log.debug("sending update on " + count + " clients for file "
			+ file.getPath());
	    }
	} catch (Exception ex) {
	    Log.warn("failed to send update for file " + file.getPath(), ex);
	}
    }

    /*@Override
    public void onFileModified(FileInstance file) {
	try {
	    final String localRelative = dir.paths.relativeFilePath(file.
		    getPath());
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
    }*/
    @Override
    @SuppressWarnings("NestedSynchronizedStatement")
    public synchronized void onFileDeleted(FileInstance file) {
	try {
	    HashSet<String> tokens = runtimeTokensForFile.remove(file.getPath());
	    if (tokens == null || tokens.isEmpty()) {
		Log.debug("deleted file " + file.getPath()
			+ " but no clients are listening");
		return;
	    }
	    int count = 0;
	    for (String token : tokens) {
		final ClientWorker worker = connectedTokensWithWorkers.
			get(token);

		if (worker != null) {
		    if (worker.addTask(new ClientTask(
			    ClientTaskType.DELETE_REMOTE_FILE, file))) {
			count++;
		    } else if (Log.isTraceEnabled()) {
			Log.trace("could not add delete file " + file.
				getPath() + " for token " + token);
		    }
		}
		final HashSet files = runtimeFilesForToken.get(token);
		synchronized (files) {
		    files.remove(file.getPath());
		}
	    }
	    if (Log.isDebugEnabled()) {
		Log.debug("notifying deletion on " + count + " client for file "
			+ file.getPath());
	    }
	} catch (Exception ex) {
	    Log.warn("failed to notify deletion for file " + file.getPath(), ex);
	}
    }

    private synchronized void connectionListenerStopped() {
	if (stopped) {
	    Log.info("connection listener stopped");
	    dir.deregisterListener();

	    if (clientWorkers.isEmpty()) {
		try {
		    dataServer.close();
		    Log.info("closed data transfer server");
		} catch (Exception ex) {
		    Log.warn("failed to close data transfer server", ex);
		}
	    }

	    for (ClientWorker worker : clientWorkers) {
		Log.info("sent stop for client " + worker.getClient().
			getStringAddress());
		worker.stopWorking();
	    }
	} else {
	    Log.fatal("connection listener unexpectedly stopped");
	    try {
		connectionListener = new ConnectionListener(this, config.
			getLocalAddress(), config.getLocalControlPort());
		connectionListener.start();
	    } catch (Exception ex) {
		Log.fatal("failed restarting connection listener", ex);
		stop();
	    }
	    Log.info("restarted connection listener");
	}
    }

    public void httpServerStopped() {
	if (stopped) {
	    Log.info("http server stopped");
	} else {
	    Log.fatal("http server unexpectedly stopped");
	    try {
		httpManager = new HttpManager(this, config.getLocalAddress(),
			config.getLocalHttpPort());
		httpManager.start();
	    } catch (Exception ex) {
		Log.fatal("failed to restart http server", ex);
		stop();
	    }
	    Log.info("restarted http server");
	}
    }

    private synchronized void clientWorkerStopped(final ClientWorker worker) {
	Log.fine("client connection " + worker.getClient() + " closed (token "
		+ worker.getToken() + ")");

	boolean wasConnected;
	if (!(wasConnected = (connectedTokensWithWorkers.remove(worker.
		getToken()) != null))) {
	    Log.debug("client " + worker.getClient() + " was not authenticated");
	}
	if (wasConnected && validTokens.contains(worker.getToken())) {
	    offlineTokens.add(worker.getToken());
	}
	if (!clientWorkers.remove(worker)) {
	    Log.warn("worker for client " + worker.getClient()
		    + " was not registered on stopped callback");
	}
	if (authenticatingClients.remove(worker.getClient())) {
	    Log.debug("client " + worker.getClient()
		    + " stopped during authentication");
	}

	if (!connectionListener.isActive() && clientWorkers.isEmpty()) {
	    try {
		dataServer.close();
		Log.info("closed data transfer server");
	    } catch (Exception ex) {
		Log.warn("failed to close data transfer server", ex);
	    }
	}
    }

    private synchronized void refusedClient(final ClientWorker worker) {
	Log.fine("client connection " + worker.getClient().
		getStringAddress() + " refused and closed");

	if (!clientWorkers.remove(worker)) {
	    Log.warn("worker for client " + worker.getClient()
		    + " was not registered on refused callback");
	}
	if (authenticatingClients.remove(worker.getClient())) {
	    Log.debug("client " + worker.getClient()
		    + " refused during authentication");
	}
    }

    private synchronized void incomingConnection(final Socket client) {

	Log.fine("incoming connection from " + client.getRemoteSocketAddress());
	ConnectionWrapper clientWrapper;
	ClientWorker clientWorker;
	try {
	    clientWrapper = new ConnectionWrapper(client, this);
	    clientWorker = new ClientWorker(clientWrapper, this);
	} catch (Exception ex) {
	    //TODO failed to attach wrapper
	    Log.warn("failed to attach client wrapper for incoming connection "
		    + client.getRemoteSocketAddress(), ex);
	    return;
	}

	if (!authenticatingClients.contains(clientWrapper)) {
	    if (authenticatingClients.size() + connectedTokensWithWorkers.
		    size() < config.getMaxClientConnections()) {
		authenticatingClients.add(clientWrapper);
		clientWorkers.add(clientWorker);
		clientWorker.start();
	    } else {
		Log.fine("refused connection " + client.getRemoteSocketAddress()
			+ " because maximum connections reached");
		try {
		    client.close();
		} catch (Exception ex) {
		    Log.info("failed closing refused client socket");
		    //TODO naspa
		}
	    }

	}
    }

    private synchronized boolean authenticatedClient(final ClientWorker worker,
	    final String authToken) {

	if (!authenticatingClients.remove(worker.getClient())) {
	    Log.warn(
		    "authenticated a client that misses from authenticating clients set ("
		    + worker.getClient() + ")");
	}
	if (connectedTokensWithWorkers.containsKey(authToken)) {
	    Log.fine("client " + connectedTokensWithWorkers.get(
		    authToken).getClient() + " already connected with token "
		    + authToken + "; connection " + worker.getClient()
		    + " refused");
	    return false;
	}
	Log.fine("authenticated client " + worker.getClient() + " with token "
		+ authToken);

	offlineTokens.remove(authToken);
	connectedTokensWithWorkers.put(authToken, worker);
	return true;
    }

    public byte[] getSecretKey() {
	return key;
    }

    public byte[] getSecretIv() {
	return iv;
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
		    HashSet<String> tokens = tokensForContent.get(
			    DirectoryPathHelper.normalizeRelativePath(file));
		    if (tokens == null) {
			tokens = new HashSet<>();
			tokensForContent.put(DirectoryPathHelper.
				normalizeRelativePath(file), tokens);
		    }
		    if (group.getClients() != null) {
			tokens.addAll(Arrays.asList(group.getClients()));
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
	    contentForToken.put(token, new HashSet());
	    runtimeFilesForToken.put(token, new HashSet());
	}

	for (Entry<String, HashSet<String>> current : tokensForContent.
		entrySet()) {
	    for (Entry<String, HashSet<String>> other : tokensForContent.
		    entrySet()) {
		if (current.getKey().equals(other.getKey())) {
		    continue;
		}

		if ((current.getKey().startsWith(other.getKey() + '/') && dir.
			isDirectory(other.getKey())) || "/".equals(other.
			getKey())) {
		    current.getValue().addAll(other.getValue());
		}
	    }

	    for (String client : current.getValue()) {
		contentForToken.get(client).add(current.getKey());
	    }
	    /*System.out.println(current.getKey());
	    for (String token : current.getValue()) {
		System.out.print(token + " ");
	    }
	    System.out.println();*/
	}
    }

    private synchronized void processConfigurationAtRuntime() throws Exception {
	runtimeFilesForToken.clear();
	runtimeTokensForFile.clear();
	dir.setCheckPeriod(config.getFileSettings().getCheckPeriodMillis());
	dir.setMaxDepth(config.getFileSettings().getMaxDirectoryDepth());
	dir.checkWorker();
	Log.setLevel(config.getLogLevel());

	processFileSettings();

	dir.loadFiles();
	for (Entry<String, ClientWorker> entry : connectedTokensWithWorkers.
		entrySet()) {
	    if (!validTokens.contains(entry.getKey())) {
		Log.fine("closing connection " + entry.getValue().getClient()
			+ " because token " + entry.getKey()
			+ " is no longer valid");
		entry.getValue().stopWorking();
	    } else {
		entry.getValue().addTask(new ClientTask(
			ClientTaskType.GLOBAL_CHECK, null));
	    }
	}
    }

    //<editor-fold desc="RUNTIME EXTERNAL API">
    public Set<ConnectionWrapper> getConnectedClients() {
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
	processConfigurationAtRuntime();
	writeConfig();
    }

    public ServerConfiguration getConfig() {
	return config;
    }

    public void pushClient(final String token) {
	if (!connectedTokensWithWorkers.get(token).addTask(new ClientTask(
		ClientTaskType.GLOBAL_CHECK, null))) {
	    Log.warn("(web request) push failed for " + token);
	}
    }

    //</editor-fold>
    private static class ConnectionListener
	    extends Thread {

	private final CoreManager parent;
	private final ServerSocket serverSocket;
	private Address listenerAddress;
	private boolean isActive;

	public ConnectionListener(final CoreManager parent, final String address,
		int port) throws Exception {
	    this.parent = parent;
	    isActive = false;
	    serverSocket = new ServerSocket();
	    serverSocket.bind(new InetSocketAddress(address, port));
	    try {
		serverSocket.setSoTimeout(4000);
	    } catch (Exception ex) {
		Log.warn(
			"failed setting socket timeout for connection listener");
	    }
	    isActive = true;
	}

	public boolean isActive() {
	    return isActive;
	}

	@Override
	public void run() {

	    listenerAddress = new Address(serverSocket.getLocalSocketAddress().
		    toString(), serverSocket.getLocalPort());
	    Log.info("listening to address " + listenerAddress);

	    while (isActive) {

		Socket clientConnection = null;
		try {
		    clientConnection = serverSocket.accept();
		} catch (SocketTimeoutException ex) {
		    //nimic
		} catch (Exception ex) {
		    Log.fatal("encountered exception in connection listener",
			    ex);
		    isActive = false;
		    try {
			serverSocket.close();
		    } catch (Exception ex1) {
			Log.warn("failed closing connection listener socket",
				ex);
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
	private ConnectionWrapper client;
	private CoreManager manager;
	private LinkedBlockingQueue<ClientTask> tasks;
	private String authToken;
	private boolean working;

	protected ClientWorker(final ConnectionWrapper client,
		final CoreManager manager) {
	    this.client = client;
	    this.manager = manager;
	    tasks = new LinkedBlockingQueue<>();
	    tasks.add(new ClientTask(ClientTaskType.GLOBAL_CHECK, null));
	    working = true;
	}

	@Override
	public void run() {
	    try {
		authToken = client.testConnection();
		if (authToken == null) {
		    stopWorking();
		    tasks = null;
		    client.disconnect();
		    manager.refusedClient(this);
		    return;
		}
	    } catch (MustResetConnectionException ex) {
		Log.fine("failed authenticating client " + client.
			getStringAddress(), ex);
		stopWorking();
		tasks = null;
		client.disconnect();
		manager.refusedClient(this);
		return;
	    } catch (Exception ex) {
		Log.warn("exception while authenticating client "
			+ client.getStringAddress(), ex);
		stopWorking();
		tasks = null;
		client.disconnect();
		manager.refusedClient(this);
		return;
	    }

	    if (!manager.validTokens.contains(authToken)) {
		Log.fine("client " + client.getStringAddress()
			+ " submitted not valid token " + authToken);
		stopWorking();
		tasks = null;
		client.disconnect();
		manager.refusedClient(this);
		return;
	    } else if (manager.blockedTokens.contains(authToken)) {
		Log.fine("client " + client.getStringAddress()
			+ " submitted blocked token " + authToken);
		stopWorking();
		tasks = null;
		client.disconnect();
		manager.refusedClient(this);
		return;
	    } else if (!manager.authenticatedClient(this, authToken)) {
		stopWorking();
		tasks = null;
		client.disconnect();
		manager.refusedClient(this);
		return;
	    }
	    client.setAuthToken(authToken);

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
			    Log.fine(
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
		} catch (MustResetConnectionException ex) {
		    Log.fine("failed test connection for client " + client, ex);
		    stopWorking();
		    client.disconnect();
		    manager.clientWorkerStopped(this);
		    client = null;
		    manager = null;
		    tasks = null;
		    return;
		} catch (Exception ex) {
		    Log.warn("encountered exception while working for client "
			    + client, ex);
		    //ex.printStackTrace(System.err);
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
		    Log.warn(
			    "failed processing task (remaining tasks: " + tasks.
				    size() + ")", ex);
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

	public ConnectionWrapper getClient() {
	    return client;
	}

	public boolean addTask(final ClientTask task) {
	    if (working) {
		try {
		    tasks.offer(task, WAIT_TIME, TimeUnit.MILLISECONDS);
		} catch (Exception ex) {
		    Log.warn("failed passing task " + task.type + " to client "
			    + client + " with token " + authToken);
		    return false;
		}
		return true;
	    } else {
		return false;
	    }
	}

	private void executeTask(final ClientTask task) throws Exception {
	    try {
		client.setUpdating(true);
		if (Log.isTraceEnabled()) {
		    Log.trace("executing " + task.type + " task for client "
			    + authToken);
		}
		switch (task.type) {

		    case DELETE_REMOTE_FILE: {
			final FileInstance file = (FileInstance) task.object;
			client.deleteRemoteFile(manager.dir.paths.
				relativeFilePath(
					file.getPath()));
			break;
		    }

		    case UPDATE_REMOTE_FILE: {
			final FileInstance file = (FileInstance) task.object;
			client.updateRemoteFile(manager.dir.paths.
				relativeFilePath(file.getPath()), file.getPath());
			break;
		    }

		    case CHECK_FILE: {
			final String file = (String) task.object;
			final byte[] md5 = manager.dir.getCheckSum(file);
			if (md5 == null) {
			    Log.info("could not find checksum for " + file);
			    break;
			}

			boolean mustUpdate;
			try {
			    mustUpdate = !client.checkRemoteFileMD5(
				    manager.dir.paths.relativeFilePath(file),
				    md5);
			} catch (MustResetConnectionException ex) {
			    Log.debug("failed remote file check for "
				    + file);
			    break;
			} catch (Exception ex) {
			    Log.warn("failed remote file check for "
				    + file);
			    break;
			}

			if (mustUpdate) {
			    client.updateRemoteFile(manager.dir.paths.
				    relativeFilePath(file), file);
			    addTask(new ClientTask(ClientTaskType.CHECK_FILE,
				    file));
			}
			break;
		    }

		    case GLOBAL_CHECK: {
			final HashSet<String> files
				= manager.runtimeFilesForToken.
					get(authToken);
			synchronized (files) {
			    if (files == null || files.isEmpty()) {
				Log.debug("empty runtime file list for client "
					+ authToken);
				return;
			    }
			    for (String file : files) {
				if (Log.isTraceEnabled()) {
				    Log.trace("checking file " + file
					    + " for client "
					    + authToken);
				}
				final byte[] md5 = manager.dir.getCheckSum(file);
				if (md5 == null) {
				    Log.info("could not find checksum for "
					    + file);
				    continue;
				}

				boolean mustUpdate;
				try {
				    mustUpdate = !client.checkRemoteFileMD5(
					    manager.dir.paths.relativeFilePath(
						    file),
					    md5);
				} catch (MustResetConnectionException ex) {
				    Log.debug("failed remote file check for "
					    + file);
				    break;
				} catch (Exception ex) {
				    Log.warn("failed remote file check for "
					    + file);
				    break;
				}

				if (mustUpdate) {
				    client.updateRemoteFile(manager.dir.paths.
					    relativeFilePath(file), file);
				    addTask(new ClientTask(
					    ClientTaskType.CHECK_FILE, file));
				}
			    }
			}
			Log.debug("finished global check for client "
				+ authToken);
			break;
		    }
		}
	    } finally {
		client.setUpdating(false);
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
