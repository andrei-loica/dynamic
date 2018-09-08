package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.ServerConfiguration;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Andrei
 */
public class ParamsView
	extends View {

    private final CoreManager core;

    public ParamsView(final HttpManager manager, final CoreManager core) {
	super(manager);
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
	final String path = req.getRequestURI().getPath();

	if (path == null
		|| (!path.equals("/params") && !path.equals("/params/"))) {
	    respond404(req);
	    return;
	}
	if (!authProcess(req, "/params")) {
	    req.close();
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

	final ServerConfiguration config = core.getConfig();

	String content = headWithCss() + "<body>" + menu(3)
		+ "<div id=\"params-content\"><div class=\"description\"><div>Configuration parameters</div></div><form action=\"/update/configuration\" method=\"GET\" enctype=\"application/x-www-form-urlencoded\"><div class=\"part\"><ul><li>Port for connection listener:</li><li>Port for web server:</li><li>Maximum client connections:</li><li>Logging level:</li><li>Maximum directory checking depth:</li><li class=\"last\">Content checking period (milliseconds):</li></ul></div><div class=\"part\" id=\"param-values\"><div><input type=\"number\" name=\"localControlPort\" value=\""
		+ config.getLocalControlPort()
		+ "\" min=\"0\" max=\"65535\"></div><div><input type=\"number\" name=\"localHttpPort\" value=\""
		+ config.getLocalHttpPort()
		+ "\" min=\"0\" max=\"65535\"></div><div><input type=\"number\" name=\"maxClientConnections\" value=\""
		+ config.getMaxClientConnections()
		+ "\" min=\"0\"></div><div><select name=\"logLevel\"><option value=\"OFF\" title=\"Logging disabled\""
		+ ((config.getLogLevel().equals("OFF")) ? "selected" : "")
		+ ">OFF</option><option value=\"TRACE\" title=\"Log everything\""
		+ ((config.getLogLevel().equals("TRACE")) ? "selected" : "")
		+ ">TRACE</option><option value=\"DEBUG\" title=\"Log everything except success file operations for each client\""
		+ ((config.getLogLevel().equals("DEBUG")) ? "selected" : "")
		+ ">DEBUG</option><option value=\"FINE\" title=\"Like DEBUG but no file related messages\""
		+ ((config.getLogLevel().equals("FINE")) ? "selected" : "")
		+ ">FINE</option><option value=\"INFO\" title=\"Like FINE but no client connection runtime related messages\""
		+ ((config.getLogLevel().equals("INFO")) ? "selected" : "")
		+ ">INFO</option><option value=\"WARNING\" title=\"Log only minor or fatal runtime problems\""
		+ ((config.getLogLevel().equals("WARNING")) ? "selected" : "")
		+ ">WARNING</option><option value=\"FATAL\" title=\"Log only fatal runtime problems\""
		+ ((config.getLogLevel().equals("FATAL")) ? "selected" : "")
		+ ">FATAL</option>"
		+ "</select></div><div><input type=\"number\" name=\"maxDepth\" value=\""
		+ config.getFileSettings().getMaxDirectoryDepth()
		+ "\" min=\"1\"></div><div class=\"last\"><input type=\"number\" name=\"checkPeriod\" value=\""
		+ config.getFileSettings().getCheckPeriodMillis()
		+ "\" min=\"0\"></div></div><div id=\"submit-params\"><input type=\"submit\" value=\"Update\"></div></form></div></body>";

	out.write(content.getBytes());
    }

}
