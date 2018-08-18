package andrei.dynamic.server.http;

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

    @Override
    public void handle(HttpExchange req) throws IOException {
	switch (req.getRequestURI().getRawPath()) {
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
		    System.err.println("failed to upload css file");
		    //ex.printStackTrace(System.err);
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
		    System.err.println("failed to upload javascript file");
		    ex.printStackTrace(System.err);
		    respond404(req);
		}

		break;
		
	    default:
		respond404(req);
		
	}
    }

}
