package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Andrei
 */
public abstract class View
	implements HttpHandler {

    protected void respond404(final HttpExchange req) throws IOException {

	if (Log.isDebugEnabled()){
	    Log.debug("unknown or failed request to " + req.getRequestURI());
	}
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

    public String[] parsePair(final String entry) {
	int i = entry.indexOf('=');

	if (i > 0) {
	    final String[] pair = new String[2];
	    pair[0] = entry.substring(0, i);
	    pair[1] = entry.substring(i + 1);

	    return pair;
	}

	return null;
    }

    protected String headWithCss() {
	return "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css\"></head>";
    }

    protected String headWithCssAndJs() {
	return "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css\"><script src=\"/static/js\"></script></head>";
    }

    protected String menu(int selected) {
	switch (selected) {
	    case 2:
		return "<ul id=\"menu\"><a href=\"/connections\"><li>Connections</li></a><a href=\"\"><li class=\"selected\">Files</li></a><a href=\"/params\"><li>Params</li></a></ul>";
	    case 3:
		return "<ul id=\"menu\"><a href=\"/connections\"><li>Connections</li></a><a href=\"/files\"><li>Files</li></a><a href=\"\"><li class=\"selected\">Params</li></a></ul>";

	    default:
		return "<ul id=\"menu\"><a href=\"\"><li class=\"selected\">Connections</li></a><a href=\"/files\"><li>Files</li></a><a href=\"/params\"><li>Params</li></a></ul>";
	}

    }

    protected List<String> parsePostRequestQuery(final HttpExchange req) throws
	    Exception {

	final BufferedInputStream in = new BufferedInputStream(req.
		getRequestBody());
	final byte[] buff = new byte[8192];
	int totalRead = 0;
	int lastRead;

	try {
	    while ((lastRead = in.read(buff, totalRead, 8192 - totalRead)) != -1) {
		totalRead += lastRead;
		if (totalRead > 8191) {
		    break;
		}
	    }
	} catch (Exception ex) {
	    return null;
	}

	final String decoded = URLDecoder.decode(new String(buff), "UTF-8").
		trim();
	if (decoded.indexOf('&') == -1) {
	    return Arrays.asList(decoded.split(";"));
	}

	return Arrays.asList(decoded.split("&"));

    }

}
