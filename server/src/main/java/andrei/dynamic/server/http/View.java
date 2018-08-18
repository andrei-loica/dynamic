package andrei.dynamic.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
    
    protected List<String> parsePostRequestQuery(final HttpExchange req){
	
	final ArrayList<String> result = new ArrayList<>();
	
	final BufferedInputStream in = new BufferedInputStream(req.getRequestBody());
	final byte[] buff = new byte[1024];
	
	try {
	    in.mark(1024);
	    
	} catch (Exception ex){
	    return null;
	}
	
	return result;
	
    }

}
