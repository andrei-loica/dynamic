package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Andrei
 */
public class StaticView
	extends View {
    
    private static final String CSS_FILE = "/style.css";
    private static final String JS_FILE = "/script.js";
    private static final String AUTH_JS_FILE = "/auth.js";
    private static final String FAVICON = "/favicon.ico";
    
    public StaticView(final HttpManager manager){
	super(manager);
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
	switch (req.getRequestURI().getRawPath()) {
	    case "/static/favicon":
	    case "/static/favicon/":
		try {
		    req.getResponseHeaders().set("Content-Type", "image/png");
		    final OutputStream out = req.getResponseBody();
		    final BufferedInputStream file = new BufferedInputStream(
			    getClass().getResourceAsStream(FAVICON));

		    final byte[] buff = new byte[1024 * 4];
		    int read = 0;
		    req.sendResponseHeaders(200, 0);
		    while ((read = file.read(buff)) > -1) {
			out.write(buff, 0, read);
		    }
		    
		    file.close();
		    req.close();
		} catch (Exception ex) {
		    Log.debug("failed to upload favicon", ex);
		    respond404(req);
		}

		break;
		
	    case "/static/css":
	    case "/static/css/":
		try {
		    req.getResponseHeaders().set("Content-Type", "text/css");
		    final OutputStream out = req.getResponseBody();
		    final BufferedInputStream file = new BufferedInputStream(
			    getClass().getResourceAsStream(CSS_FILE));

		    final byte[] buff = new byte[1024 * 4];
		    int read = 0;
		    req.sendResponseHeaders(200, 0);
		    while ((read = file.read(buff)) > -1) {
			out.write(buff, 0, read);
		    }
		    
		    file.close();
		    req.close();
		} catch (Exception ex) {
		    Log.debug("failed to upload css file", ex);
		    respond404(req);
		}

		break;
		
	    case "/static/js":
	    case "/static/js/":
		try {
		    req.getResponseHeaders().set("Content-Type", "text");
		    final OutputStream out = req.getResponseBody();
		    final BufferedInputStream file = new BufferedInputStream(
			    getClass().getResourceAsStream(JS_FILE));

		    final byte[] buff = new byte[1024 * 4];
		    int read = 0;
		    req.sendResponseHeaders(200, 0);
		    while ((read = file.read(buff)) > -1) {
			out.write(buff, 0, read);
		    }
		    
		    file.close();
		    req.close();
		} catch (Exception ex) {
		    Log.debug("failed to upload javascript file", ex);
		    respond404(req);
		}

		break;
		
	    case "/static/auth":
	    case "/static/auth/":
		try {
		    req.getResponseHeaders().set("Content-Type", "text");
		    final OutputStream out = req.getResponseBody();
		    final BufferedInputStream file = new BufferedInputStream(
			    getClass().getResourceAsStream(AUTH_JS_FILE));

		    final byte[] buff = new byte[1024 * 4];
		    int read = 0;
		    req.sendResponseHeaders(200, 0);
		    while ((read = file.read(buff)) > -1) {
			out.write(buff, 0, read);
		    }
		    
		    file.close();
		    req.close();
		} catch (Exception ex) {
		    Log.debug("failed to upload auth javascript file", ex);
		    respond404(req);
		}

		break;
		
	    default:
		respond404(req);
		
	}
    }

}
