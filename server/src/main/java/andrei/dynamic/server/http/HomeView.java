package andrei.dynamic.server.http;

import andrei.dynamic.server.ClientWithServer;
import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author Andrei
 */
public class HomeView
	extends View {

    private final CoreManager core;

    public HomeView(final CoreManager core) {
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {

	final String path = req.getRequestURI().getPath();

	if (path == null || (!path.equals("/") && !path.equals("/home/")
		&& !path.equals("/home"))) {
	    respond404(req);
	    return;
	}

	OutputStream out = req.getResponseBody();
	Headers headers = req.getResponseHeaders();
	headers.set("Content-Type", "text/html");
	headers.set("Refresh", "3");
	req.sendResponseHeaders(200, 0);

	writePage(out);

	req.close();

    }

    private void writePage(final OutputStream out) throws IOException {

	final Set<String> connected = core.getConnectedClients();

	String content = "<br><p>Connected clients:&nbsp&nbsp" + connected.
		size()
		+ " / " + core.getMaxClientConnections()
		+ " <i><a href=\"/update/maxClientConnections\">(Change limit)</a></i></p><hr><br><br><ul>";

	for (String client : connected) {
	    content = content + "<li>" + client
		    + " <i><a href=\"/update/close_connection?client=" + client
		    + "\">(close connection)</a></i></li>";
	}

	content = content + "</ul><br><br>";
	out.write(content.getBytes());
    }

}
