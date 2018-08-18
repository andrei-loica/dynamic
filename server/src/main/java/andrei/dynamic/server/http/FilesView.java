package andrei.dynamic.server.http;

import andrei.dynamic.server.CoreManager;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.server.jaxb.XmlFileGroupElement;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Andrei
 */
public class FilesView extends View{

    private final CoreManager core;

    public FilesView(final CoreManager core) {
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {
	final String path = req.getRequestURI().getPath();

	if (path == null || (!path.equals("/files") && !path.equals("/files/"))) {
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

	final ArrayList<XmlFileGroup> groups = core.getFileGroups();
	
	String content = headWithCssAndJs() + "<body>" + menu(2) + "<div id=\"files-content\"><div class=\"description\"><span>Configured file groups&nbsp&nbsp</span><button id=\"vision-button\" onClick=\"changeNewGroupVision()\"> + </button></div><form style=\"display: none\" id=\"new-group\" action=\"/update/files\" method=\"POST\"><label>Clients:</label> <button id=\"add-button\" type=\"button\" onClick=\"addClientNode('0_c')\"> + </button><div id=\"0_c\"><input type=\"hidden\" name=\"idx\" value=\"0\"><input type=\"text\" name=\"client\"></div><br><label>Files:</label> <button id=\"add-button\" type=\"button\" onClick=\"addFileNode('0_f')\"> + </button><div id=\"0_f\"><input type=\"text\" name=\"file\"></div><input type=\"submit\" value=\"Add\"></form>";
	
	for (XmlFileGroup group : groups){
	    content = content + "<form action=\"/update/files\" method=\"POST\" enctype=\"application/x-www-form-urlencoded\"><label>Clients:</label> <button id=\"add-button\" type=\"button\" onClick=\"addClientNode('" + group.getOrder() + "_c')\"> + </button><div id=\"" + group.getOrder() + "_c\"><input type=\"hidden\" name=\"idx\" value=\"" + group.getOrder() + "\">";
	    
	    for (String client : group.getClients()){
		content = content + "<input type=\"text\" name=\"client\" value=\"" + client + "\">";
	    }
	    content = content + "</div><br><label>Files:</label> <button id=\"add-button\" type=\"button\" onClick=\"addFileNode('" + group.getOrder() + "_f')\"> + </button><div id=\"" + group.getOrder() + "_f\">";
	    
	    for (XmlFileGroupElement file : group.getFiles()){
		content = content + "<input type=\"text\" name=\"file\" value=\"" + file.getLocalPath() + "\">";
	    }
	    content = content + "</div><input type=\"submit\" value=\"Update\"></form>";
	}
	
	content = content + "</div></body>";
	out.write(content.getBytes());
    }
    
}
