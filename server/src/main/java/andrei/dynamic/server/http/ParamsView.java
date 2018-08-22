package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.ServerConfiguration;
import andrei.dynamic.server.jaxb.XmlServerConfiguration;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Andrei
 */
public class ParamsView extends View{

    private final CoreManager core;

    public ParamsView(final CoreManager core) {
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
	final String path = req.getRequestURI().getPath();

	if (path == null || (!path.equals("/params") && !path.equals("/params/"))) {
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
	
	final ServerConfiguration config = core.getConfig();
	
	String content = headWithCss() + "<body>" + menu(3) + "<div id=\"params-content\"><div class=\"description\"><div>Configuration parameters</div></div><form action=\"/update/configuration\" method=\"GET\" enctype=\"application/x-www-form-urlencoded\"><div class=\"part\"><ul><li>IP port for connection listener:</li><li>IP port for file transfer:</li><li>IP port for Web Server:</li><li>Maximum client connections:</li><li>Maximum directory checking depth:</li><li class=\"last\">Content checking period (milliseconds):</li></ul></div><div class=\"part\" id=\"param-values\"><div><input type=\"number\" name=\"localControlPort\" value=\""
		+ config.getLocalControlPort() + "\" min=\"0\" max=\"65535\"></div><div><input type=\"number\" name=\"localDataPort\" value=\"" + config.getLocalDataPort() + "\" min=\"0\" max=\"65535\"></div><div><input type=\"number\" name=\"localHttpPort\" value=\"" + config.getLocalHttpPort() + "\" min=\"0\" max=\"65535\"></div><div><input type=\"number\" name=\"maxClientConnections\" value=\"" + config.getMaxClientConnections() + "\" min=\"0\"></div><div><input type=\"number\" name=\"maxDepth\" value=\"" + config.getFileSettings().getMaxDirectoryDepth() + "\" min=\"0\"></div><div class=\"last\"><input type=\"number\" name=\"checkPeriod\" value=\"" + config.getFileSettings().getCheckPeriodMillis() + "\" min=\"0\"></div></div><div id=\"submit-params\"><input type=\"submit\" value=\"Update\"></div></form></div></body>";
	
	out.write(content.getBytes());
    }
    
}
