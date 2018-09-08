package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 *
 * @author Andrei
 */
public class FilesView
	extends View {

    private final CoreManager core;

    public FilesView(final HttpManager manager, final CoreManager core) {
	super(manager);
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
	final String path = req.getRequestURI().getPath();

	if (path == null || (!path.equals("/files") && !path.equals("/files/"))) {
	    respond404(req);
	    return;
	}
	if (!authProcess(req, "/files")) {
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

	final ArrayList<XmlFileGroup> groups = core.getFileGroups();

	String content = headWithCssAndJs() + "<body>" + menu(2)
		+ "<div id=\"files-content\"><div class=\"description\"><div>Configured file groups&nbsp&nbsp</div><div><button id=\"vision-button\" onClick=\"changeNewGroupVision()\"> + </button></div></div><form style=\"display: none\" id=\"new-group\" action=\"/update/files\" method=\"POST\"><label>Clients:</label> <button id=\"add-button\" type=\"button\" onClick=\"addClientNode('0_c')\"> + </button><div id=\"0_c\"><input type=\"hidden\" name=\"idx\" value=\"0\"><input type=\"text\" name=\"client\"></div><br><label>Files:</label> <button id=\"add-button\" type=\"button\" onClick=\"addFileNode('0_f')\"> + </button><div id=\"0_f\"><input type=\"text\" name=\"file\"></div><input type=\"submit\" value=\"Add\"></form>";

	for (XmlFileGroup group : groups) {
	    content = content
		    + "<form action=\"/update/files\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\"><label>Clients:</label> <button id=\"add-button_"
		    + group.getOrder()
		    + "_c\" type=\"button\" onClick=\"addClientNode('"
		    + group.getOrder() + "_c')\"> + </button><div id=\""
		    + group.getOrder()
		    + "_c\"><input type=\"hidden\" name=\"idx\" value=\""
		    + group.getOrder() + "\">";
	    if (group.getClients() == null) {
		content = content
			+ "<input type=\"text\" name=\"client\" value=\"\" maxlength=\"255\">";
	    } else {
		for (String client : group.getClients()) {
		    content = content
			    + "<input type=\"text\" name=\"client\" value=\""
			    + client + "\" maxlength=\"255\">";
		}
	    }
	    content = content
		    + "</div><br><label>Files:</label> <button id=\"add-button_"
		    + group.getOrder()
		    + "_f\" type=\"button\" onClick=\"addFileNode('"
		    + group.getOrder() + "_f')\"> + </button><div id=\""
		    + group.getOrder() + "_f\">";

	    if (group.getFiles() == null) {
		content = content
			+ "<input type=\"text\" name=\"file\" value=\"\" maxlength=\"150\">";
	    } else {
		for (String file : group.getFiles()) {
		    content = content
			    + "<input type=\"text\" name=\"file\" value=\""
			    + file
			    + "\" maxlength=\"150\">";
		}
	    }
	    content = content
		    + "</div><input type=\"submit\" value=\"Update\"></form>";
	}

	content = content + "</div></body>";
	out.write(content.getBytes());
    }

}
