package andrei.dynamic.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Andrei
 */
public abstract class View
	implements HttpHandler {

    protected void respond404(final HttpExchange req) throws IOException {

	OutputStream out = req.getResponseBody();
	String message = "ERROR - Resource not found";
	req.sendResponseHeaders(404, message.length());

	out.write(message.getBytes());

	req.close();
    }

    protected void respond404(final HttpExchange req, final String message)
	    throws IOException {

	OutputStream out = req.getResponseBody();
	req.sendResponseHeaders(404, message.length());

	out.write(message.getBytes());

	req.close();
    }

    protected void redirectTo(final String location, final HttpExchange req)
	    throws IOException {
	Headers headers = req.getResponseHeaders();
	headers.add("Location", location);
	req.sendResponseHeaders(302, 0);

	req.close();
    }

}
