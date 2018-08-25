package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.ServerConfiguration;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Andrei
 */
public class UpdateView
	extends View {

    private final CoreManager core;

    public UpdateView(final CoreManager core) {
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {

	String path = req.getRequestURI().getRawPath();

	if (path == null || !path.startsWith("/update") || path.length() > 300) { //arbitrary value for safety
	    respond404(req);
	    return;
	}

	path = path.substring(7);

	switch (path) {
	    case "/files":
	    case "/files/":
		try {
		    final HashSet<String> clients = new HashSet();
		    final ArrayList<String> files = new ArrayList();
		    int idx = -1;

		    for (String entry : parsePostRequestQuery(req)) {
			final String[] pair = parsePair(entry);

			switch (pair[0]) {
			    case "client":
				if (pair[1].length()
					> MessageFactory.STD_MSG_DIM - 1) {
				    respond404(req);
				    return;
				}
				if (pair[1].length() > 0) {
				    clients.add(pair[1]);
				}
				break;

			    case "file":
				if (pair[1].length()
					> MessageFactory.STD_MSG_DIM - 1) {
				    respond404(req);
				    return;
				}
				if (pair[1].length() > 0) {
				    files.add(pair[1]);
				}
				break;

			    case "idx":
				if (idx != -1) {
				    respond404(req);
				    return;
				}
				idx = Integer.parseInt(pair[1]);
				break;

			    default:
				respond404(req);
				return;

			}
		    }

		    Log.info("(web request) updating file-groups");
		    final ServerConfiguration config = core.getConfig();
		    if (idx > 0) {
			if (clients.isEmpty() && files.isEmpty()) {
			    synchronized (config) {
				config.getFileSettings().getGroups().remove(idx
					- 1);
				XmlFileGroup.index--;
				core.saveConfig();
			    }
			    redirectTo("/files", req);
			    return;
			}
			synchronized (config) {
			    final XmlFileGroup group = config.getFileSettings().
				    getGroups().get(
					    idx - 1);
			    group.setClients(Arrays.copyOf(clients.toArray(),
				    clients.size(), String[].class));
			    group.setFiles(Arrays.copyOf(files.toArray(), files.
				    size(), String[].class));
			    core.saveConfig();
			}
		    } else if (!clients.isEmpty() && !files.isEmpty()) {
			synchronized (config) {
			    final XmlFileGroup group = new XmlFileGroup();
			    group.setClients(Arrays.copyOf(clients.toArray(),
				    clients.size(), String[].class));
			    group.setFiles(Arrays.copyOf(files.toArray(), files.
				    size(), String[].class));
			    group.setOrder(++XmlFileGroup.index);
			    config.getFileSettings().getGroups().add(group);
			    core.saveConfig();
			}

		    }
		} catch (Exception ex) {
		    Log.warn("(web request) failed to update file groups", ex);
		}
		redirectTo("/files", req);

		break;

	    case "/configuration":
	    case "/configuration/":
		try {
		    int localControlPort = -1;
		    int localDataPort = -1;
		    int localHttpPort = -1;
		    int maxClientConnections = -1;
		    int maxDepth = -1;
		    int checkPeriod = -1;
		    String logLevel = null;

		    final List<String> query = parsePostRequestQuery(req);
		    for (String entry : req.getRequestURI().getQuery().
			    split("&")) {
			final String[] pair = parsePair(entry);

			switch (pair[0]) {
			    case "localControlPort":
				localControlPort = Integer.parseInt(pair[1]);
				break;

			    case "localDataPort":
				localDataPort = Integer.parseInt(pair[1]);
				break;

			    case "localHttpPort":
				localHttpPort = Integer.parseInt(pair[1]);
				break;

			    case "maxClientConnections":
				maxClientConnections = Integer.parseInt(pair[1]);
				break;

			    case "maxDepth":
				maxDepth = Integer.parseInt(pair[1]);
				break;

			    case "checkPeriod":
				checkPeriod = Integer.parseInt(pair[1]);
				break;

			    case "logLevel":
				logLevel = pair[1];
				break;
			}
		    }

		    final ServerConfiguration config = core.getConfig();
		    synchronized (config) {
			if (localControlPort >= 0 && localControlPort < 65536) {
			    config.setLocalControlPort(localControlPort);
			}
			if (localDataPort >= 0 && localDataPort < 65536) {
			    config.setLocalDataPort(localDataPort);
			}
			if (localHttpPort >= 0 && localHttpPort < 65536) {
			    config.setLocalHttpPort(localHttpPort);
			}
			if (maxClientConnections >= 0) {
			    config.setMaxClientConnections(maxClientConnections);
			}
			if (maxDepth >= 0) {
			    config.getFileSettings().setMaxDirectoryDepth(
				    maxDepth);
			}
			if (checkPeriod >= 0) {
			    config.getFileSettings().setCheckPeriodMillis(
				    checkPeriod);
			}
			if (logLevel != null) {
			    config.setLogLevel(logLevel);
			}
			Log.info("(web request) updating configuration");
			core.saveConfig();
		    }
		} catch (Exception ex) {
		    Log.warn("(web request) failed to update configuration", ex);
		}
		redirectTo("/params", req);

		break;

	    default:
		respond404(req);
	}
    }

}
