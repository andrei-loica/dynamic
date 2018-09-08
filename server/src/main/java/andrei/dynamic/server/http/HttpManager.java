package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;

/**
 *
 * @author Andrei
 */
public class HttpManager {

    private final CoreManager core;
    private final HttpServer httpServer;
    private Worker worker;
    private final String id;
    private final String password;
    private final int authExpTime;
    private final boolean useAuth;
    private boolean working;

    public HttpManager(final CoreManager core, final String httpAddress,
	    int httpPort, final String id, final String password,
	    int authExpTime) throws Exception {
	this.core = core;

	httpServer = HttpServer.create(new InetSocketAddress(httpAddress,
		httpPort), 0);
	if (id == null || id.isEmpty() || authExpTime < 1) {
	    this.id = null;
	    this.password = null;
	    this.authExpTime = 0;
	    worker = null;
	    useAuth = false;
	} else {
	    this.id = id;
	    if (password == null) {
		this.password = "";
	    } else {
		this.password = password;
	    }
	    this.authExpTime = authExpTime;
	    worker = new Worker(this, id, password, authExpTime * 1000);
	    useAuth = true;
	}
    }

    public void start() {
	working = true;
	httpServer.createContext("/", new ConnectionsView(this, core));
	httpServer.createContext("/files", new FilesView(this, core));
	httpServer.createContext("/params", new ParamsView(this, core));
	httpServer.createContext("/actions/", new ActionsView(this, core));
	httpServer.createContext("/update/", new UpdateView(this, core));
	httpServer.createContext("/static/", new StaticView(this));

	httpServer.setExecutor(Executors.newCachedThreadPool());
	if (worker != null) {
	    worker.start();
	}
	httpServer.start();
    }

    public void stop() {

	working = false;
	final Thread stopThread = new Thread(() -> {
	    httpServer.stop(1);
	    if (worker != null) {
		worker.stopWorking();
		try {
		    worker.interrupt();
		} catch (Exception ex) {
		    Log.debug("failed interrupting http worker thread", ex);
		}
	    } else {
		core.httpServerStopped();
	    }
	});

	stopThread.start();
    }

    public boolean checkAuth(final String address) {
	if (worker != null) {
	    return worker.checkAuth(address);
	}

	return true;
    }

    public String[] getCredentials() {
	if (worker != null) {
	    return worker.getCredentials();
	}

	return null;
    }

    public void authenticated(final String address) {
	if (worker != null) {
	    worker.authenticated(address);
	}
    }

    public boolean usingAuth() {
	return useAuth;
    }

    protected void workerStopped() {
	if (working) {
	    if (useAuth) {
		if (worker != null) {
		    if (!worker.isWorking()) {
			worker.stopWorking();
			worker = new Worker(this, id, password, authExpTime
				* 1000);
			worker.start();
		    }
		} else {
		    worker = new Worker(this, id, password, authExpTime
			    * 1000);
		    worker.start();
		}
	    }
	} else {
	    core.httpServerStopped();
	}
    }

    private static class Worker
	    extends Thread {

	private final HttpManager parent;
	private final String id;
	private final String pw;
	private final int expTime;
	private final HashMap<String, Long> authenticated;
	private boolean working;

	protected Worker(final HttpManager parent, final String id,
		final String pw, int authExpTime) {
	    this.parent = parent;
	    this.id = id;
	    this.pw = pw;
	    this.expTime = authExpTime;
	    authenticated = new HashMap();
	    working = true;
	}

	@Override
	public void run() {

	    while (working) {
		synchronized (this) {
		    long now = System.currentTimeMillis();
		    authenticated.entrySet().removeIf(entry -> entry.
			    getValue()
			    < now - expTime);
		}
		try {
		    Thread.sleep(expTime);
		} catch (Exception ex) {
		    //nimic
		}
	    }

	    parent.workerStopped();
	}

	public synchronized boolean isWorking() {
	    return working;
	}

	public synchronized void stopWorking() {
	    working = false;
	}

	public synchronized boolean checkAuth(final String address) {
	    Long time = authenticated.remove(address);
	    if (time != null && time > System.currentTimeMillis() - expTime) {
		authenticated.put(address, System.currentTimeMillis());
		return true;
	    } else {
		return false;
	    }
	}

	public synchronized String[] getCredentials() {
	    return new String[]{id, pw};
	}

	public synchronized void authenticated(final String address) {
	    authenticated.put(address, System.currentTimeMillis());
	    Log.info("authenticated host " + address + " to web server");
	}

    }

}
