package andrei.dynamic.server.http;

import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.ServerConfiguration;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.server.jaxb.XmlFileGroupElement;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.ArrayList;
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
		    final ArrayList<XmlFileGroupElement> files = new ArrayList();
		    int idx = -1;

		    for (String entry : req.getRequestURI().getQuery().
			    split("&")) {
			final String[] pair = parsePair(entry);

			switch (pair[0]) {
			    case "client":
				if (pair[1].length()
					> MessageFactory.TEST_MSG_DIM - 1) {
				    respond404(req);
				    return;
				}
				if (pair[1].length() > 0) {
				    clients.add(pair[1]);
				}
				break;

			    case "file":
				if (pair[1].length()
					> MessageFactory.UPDATE_FILE_MSG_DIM - 1) {
				    respond404(req);
				    return;
				}
				if (pair[1].length() > 0) {
				    XmlFileGroupElement file
					    = new XmlFileGroupElement();
				    file.setLocalPath(pair[1]);
				    file.setRemotePath(pair[1]);
				    files.add(file);
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

		    final ServerConfiguration config = core.getConfig();
		    if (idx > 0) {
			if (clients.isEmpty() && files.isEmpty()) {
			    synchronized (config) {
				config.getFileSettings().getGroups().remove(idx
					- 1);
				XmlFileGroup.index--;
				core.saveConfig();
			    }
			}
			synchronized (config) {
			    final XmlFileGroup group = config.getFileSettings().
				    getGroups().get(
					    idx - 1);
			    group.setClients((String[]) clients.toArray());
			    group.setFiles((XmlFileGroupElement[]) files.
				    toArray());
			    core.saveConfig();
			}
		    } else if (!clients.isEmpty() && !files.isEmpty()) {
			synchronized (config) {
			    final XmlFileGroup group = new XmlFileGroup();
			    group.setClients((String[]) clients.toArray());
			    group.setFiles((XmlFileGroupElement[]) files.
				    toArray());
			    group.setOrder(++XmlFileGroup.index);
			    config.getFileSettings().getGroups().add(group);
			    core.saveConfig();
			}

		    }
		} catch (Exception ex) {
		    System.err.println("failed to update files: " + ex.
			    getMessage());
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

		    final List<String> query = parsePostRequestQuery(req);
		    for (String entry : req.getRequestURI().getQuery().
			    split("&")) {
			final String[] pair = parsePair(entry);
			int value = Integer.parseInt(pair[1]);

			switch (pair[0]) {
			    case "localControlPort":
				localControlPort = value;
				break;

			    case "localDataPort":
				localDataPort = value;
				break;

			    case "localHttpPort":
				localHttpPort = value;
				break;

			    case "maxClientConnections":
				maxClientConnections = value;
				break;

			    case "maxDepth":
				maxDepth = value;
				break;

			    case "checkPeriod":
				checkPeriod = value;
				break;
			}
		    }

		    final ServerConfiguration config = core.getConfig();
		    synchronized (config) {
			if (localControlPort >= 0) {
			    config.setLocalControlPort(localControlPort);
			}
			if (localDataPort >= 0) {
			    config.setLocalDataPort(localDataPort);
			}
			if (localHttpPort >= 0) {
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
			core.saveConfig();
		    }
		} catch (Exception ex) {
		    System.err.println("failed to update configuration params: "
			    + ex.getMessage());
		    ex.printStackTrace(System.out);
		}
		redirectTo("/params", req);

		break;

	    default:
		respond404(req);
	}
    }

}
