package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import andrei.dynamic.common.MessageFactory;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;

/**
 *
 * @author Andrei
 */
public abstract class View
	implements HttpHandler {

    private static final String ALPHANUM
	    = "01abc2def3ghi4jkl5mno6pqr7stu8vwx9yz";
    private final HttpManager manager;

    public View(final HttpManager manager) {
	this.manager = manager;
    }

    protected void respond404(final HttpExchange req) throws IOException {

	if (Log.isDebugEnabled()) {
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

    protected boolean checkCredentials(final HttpExchange req, final String id,
	    final String pw, final String salt) {
	final String[] credentials = manager.getCredentials();
	if (credentials == null) {
	    return true;
	}

	final String address = req.getRemoteAddress().getHostString();
	try {
	    if (credentials[0].equals(id) && DigestUtils.
		    sha256Hex(credentials[1] + salt).equals(pw)) {
		manager.authenticated(address);
		return true;
	    }
	} catch (Exception ex) {
	    Log.debug("caught exception while authenticating " + address, ex);
	}

	return false;
    }

    protected void writeAuth(final HttpExchange req, final String redirectTo)
	    throws Exception {
	final SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
	final String salt = "" + ALPHANUM.charAt(rand.nextInt(36)) + ALPHANUM.
		charAt(rand.nextInt(36)) + ALPHANUM.charAt(rand.nextInt(36))
		+ ALPHANUM.charAt(rand.nextInt(36)) + ALPHANUM.charAt(rand.
		nextInt(36));

	String content
		= "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css\"><script src=\"/static/auth\"></script></head><body><form id=\"auth\" action=\"/actions/login\" onsubmit=\"beforeSubmit()\" method=\"POST\"><div id=\"title\"><b>Authentication required</b><br></div><div><label>Id:</label><br><input type=\"text\" name=\"id\"></div><div><label>Password:</label><br><input id=\"password\" type=\"password\" name=\"password\"></div><input id=\"salt\" name=\"salt\" type=\"hidden\" value=\""
		+ salt
		+ "\"><input type=\"hidden\" name=\"redirectTo\" value=\""
		+ redirectTo
		+ "\"><input type=\"submit\" value=\"Login\"></form><script>function beforeSubmit(){var pass = document.getElementById('password');var salt = document.getElementById('salt');pass.value = Sha256.hash(pass.value + salt.value, { outFormat: 'hex' });return true;}</script></body>";

	Headers headers = req.getResponseHeaders();
	headers.set("Content-Type", "text/html");
	req.sendResponseHeaders(200, 0);
	req.getResponseBody().write(content.getBytes());

	req.close();
    }

    protected void writeAuthAfterFailed(final HttpExchange req,
	    final String redirectTo) throws Exception {
	final SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
	final String salt = "" + ALPHANUM.charAt(rand.nextInt(36)) + ALPHANUM.
		charAt(rand.nextInt(36)) + ALPHANUM.charAt(rand.nextInt(36))
		+ ALPHANUM.charAt(rand.nextInt(36)) + ALPHANUM.charAt(rand.
		nextInt(36));

	String content
		= "<head><link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css\"><script src=\"/static/auth\"></script></head><body><form id=\"auth\" action=\"/actions/login\" onsubmit=\"beforeSubmit()\" method=\"POST\"><div id=\"title\"><b>Authentication required</b><br><i>Login failed</i></div><div><label>Id:</label><br><input type=\"text\" name=\"id\"></div><div><label>Password:</label><br><input id=\"password\" type=\"password\" name=\"password\"></div><input id=\"salt\" name=\"salt\" type=\"hidden\" value=\""
		+ salt
		+ "\"><input type=\"hidden\" name=\"redirectTo\" value=\""
		+ redirectTo
		+ "\"><input type=\"submit\" value=\"Login\"></form><script>function beforeSubmit(){var pass = document.getElementById('password');var salt = document.getElementById('salt');pass.value = Sha256.hash(pass.value + salt.value, { outFormat: 'hex' });return true;}</script></body>";

	Headers headers = req.getResponseHeaders();
	headers.set("Content-Type", "text/html");
	req.sendResponseHeaders(200, 0);
	req.getResponseBody().write(content.getBytes());

	req.close();
    }

    protected boolean authProcess(final HttpExchange req, final String redirect) {
	if (!manager.usingAuth() || manager.checkAuth(req.getRemoteAddress().
		getHostString())) {
	    return true;
	}

	try {
	    writeAuth(req, redirect);
	} catch (Exception ex) {
	    Log.debug("caught exception while loading auth page", ex);
	}

	return false;
    }

}
