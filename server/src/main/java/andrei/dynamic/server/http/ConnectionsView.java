package andrei.dynamic.server.http;

import andrei.dynamic.server.ClientWithServer;
import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Andrei
 */
public class ConnectionsView
	extends View {

    private final CoreManager core;

    public ConnectionsView(final CoreManager core) {
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {

	final String path = req.getRequestURI().getPath();

	if ("/".equals(path)) {
	    redirectTo("/connections", req);
	}

	if (path == null || (!path.equals("/connections") && !path.equals(
		"/connections/"))) {
	    respond404(req);
	    return;
	}

	OutputStream out = req.getResponseBody();
	Headers headers = req.getResponseHeaders();
	headers.set("Content-Type", "text/html");
	//headers.set("Refresh", "3");
	req.sendResponseHeaders(200, 0);

	writePage(out);

	req.close();

    }

    private void writePage(final OutputStream out) throws IOException {

	final Collection<ClientWithServer> connected;
	final Set<String> offline;
	final Set<String> blocked;

	synchronized (core) {
	    connected = core.getConnectedClients();
	    offline = core.getOfflineClients();
	    blocked = core.getBlockedClients();
	}

	final Set<String> blockedNotConfigured = new HashSet<>();
	blockedNotConfigured.addAll(blocked);
	blockedNotConfigured.removeAll(offline);

	String content
		= headWithCss() + "<body>" + menu(1)
		+ "<div id=\"conn-content\"><div class=\"description\"><div>Configured clients&nbsp&nbsp<i style=\"font-size: 15px\">("
		+ connected.size() + "/" + (connected.size() + offline.size())
		+ " connected)</i></div></div><table id=\"connections\"><tr id=\"table-description\"><th>State</th><th>Name</th><th>Connected IP Address</th><th colspan=\"3\" style=\"text-align: center\">Actions</th></tr>";

	for (ClientWithServer client : connected) {
	    blockedNotConfigured.remove(client.getAuthToken());
	    if (blocked.contains(client.getAuthToken())) {
		content = content
			+ "<tr><th class=\"aligned-cell\"><span class=\"blocked\" title=\"Blocked and closing\"></span></th><th>"
			+ client.getAuthToken() + "</th><th>" + client.
			getStringAddress()
			+ "</th></th><th class=\"aligned-cell\" colspan=\"3\"><a href=\"/actions/unblock?client="
			+ client.getAuthToken()
			+ " \" onclick=\"return confirm('Confirm unblock action?')\">unblock</a></th></tr>";
	    } else if (client.isClosing()) {
		content = content
			+ "<tr><th class=\"aligned-cell\"><span class=\"closing\" title=\"Disconnecting\"></span></th><th>"
			+ client.getAuthToken() + "</th><th>" + client.
			getStringAddress()
			+ "</th></th><th class=\"aligned-cell\" colspan=\"3\"><a href=\"/actions/block?client="
			+ client.getAuthToken()
			+ "\" onclick=\"return confirm('Confirm block action?')\">block</a></th></tr>";
	    } else {
		content = content
			+ "<tr><th class=\"aligned-cell\"><span class=\"connected\" title=\"Connected\"></span></th><th>"
			+ client.getAuthToken() + "</th><th>" + client.
			getStringAddress()
			+ "</th><th class=\"aligned-cell\"><a href=\"/actions/push?client="
			+ client.getAuthToken()
			+ "\" onclick=\"return confirm('Confirm push action?')\">push</a></th><th class=\"aligned-cell\"><a href=\"/actions/disconnect?client="
			+ client.getAuthToken()
			+ "\" onclick=\"return confirm('Confirm disconnect action?')\">disconnect</a></th><th class=\"aligned-cell\"><a href=\"/actions/block?client="
			+ client.getAuthToken()
			+ "\" onclick=\"return confirm('Confirm block action?')\">block</a></th></tr>";
	    }
	}

	for (String client : offline) {
	    if (blocked.contains(client)) {
		content = content
			+ "<tr><th class=\"aligned-cell\"><span class=\"blocked\" title=\"Blocked\"></span></th><th>"
			+ client
			+ "</th><th>N/A</th></th><th class=\"aligned-cell\" colspan=\"3\"><a href=\"/actions/unblock?client="
			+ client + "\" onclick=\"return confirm('Confirm unblock action?')\">unblock</a></th></tr>";
	    } else {
		content = content
			+ "<tr><th class=\"aligned-cell\"><span class=\"offline\" title=\"Not connected\"></span></th><th>"
			+ client
			+ "</th><th>N/A</th></th><th class=\"aligned-cell\" colspan=\"3\"><a href=\"/actions/block?client="
			+ client + "\" onclick=\"return confirm('Confirm block action?')\">block</a></th></tr>";
	    }
	}

	for (String client : blockedNotConfigured) {
	    content = content
		    + "<tr><th class=\"aligned-cell\"><span class=\"blocked\" title=\"Blocked and not configured\"></span></th><th>"
		    + client
		    + "</th><th>N/A</th></th><th class=\"aligned-cell\" colspan=\"3\"><a href=\"/actions/unblock?client="
		    + client + "\" onclick=\"return confirm('Confirm unblock action?')\">unblock</a></th></tr>";

	}

	content = content + "</table></div></body>";
	out.write(content.getBytes());
    }

}
