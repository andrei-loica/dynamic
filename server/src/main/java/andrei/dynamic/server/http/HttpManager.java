package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 *
 * @author Andrei
 */
public class HttpManager {

    private final CoreManager core;
    private final int httpPort;
    private final HttpServer httpServer;

    public HttpManager(final CoreManager core, int httpPort) throws
	    Exception {
	this.core = core;
	this.httpPort = httpPort;

	httpServer = HttpServer.create(new InetSocketAddress(httpPort), 0);
    }
    
    public void start(){
	httpServer.createContext("/", new HomeView(core));
	httpServer.createContext("/home/", new HomeView(core));
	httpServer.createContext("/update/", new UpdateView(core));
	
	httpServer.setExecutor(Executors.newCachedThreadPool());
	httpServer.start();
    }
    
    public void stop(){
	
	final Thread stopThread = new Thread(() -> {
	    httpServer.stop(1);
	    core.httpServerStopped();
	});
	
	stopThread.start();
    }

}
