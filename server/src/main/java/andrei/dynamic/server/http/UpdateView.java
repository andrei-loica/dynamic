package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

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
	    respond404(req, "ai ajuns in update view");
	    return;
	}

	path = path.substring(7);

	if (path.equals("/maxClientConnections") || path.equals("/maxClientConnections/")) {
	    if (req.getRequestURI().getRawQuery().startsWith("value=")) {
		
		final String str = req.getRequestURI().getRawQuery().
			substring(6);
		final int val;
		try {
		    val = Integer.parseInt(str);
		} catch (Exception ex) {
		    System.err.println(
			    "failed to get maxClientConnections from request query");
		    return;
		}
		core.setMaxClientConnections(val);

	    } else {
		respond404(req);
		return;
	    }
	} 

	redirectTo("/", req);
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
