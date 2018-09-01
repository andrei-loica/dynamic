package andrei.dynamic.server.http;

import andrei.dynamic.common.Log;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.server.CoreManager;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

/**
 *
 * @author Andrei
 */
public class ActionsView
	extends View {

    private final CoreManager core;

    public ActionsView(final HttpManager manager, final CoreManager core) {
	super(manager);
	this.core = core;
    }

    @Override
    public void handle(HttpExchange req) throws IOException {

	String path = req.getRequestURI().getRawPath();

	if (path == null || !path.startsWith("/actions") || path.length() > 300) { //arbitrary value for safety
	    respond404(req);
	    return;
	}

	path = path.substring(8);
	final String address;
	try {
	    address = req.getRemoteAddress().getHostString();
	} catch (Exception ex) {
	    Log.debug("failed to get remote address in web request");
	    req.close();
	    return;
	}

	switch (path) {
	    case "/disconnect":
	    case "/disconnect/": {
		if (!authProcess(req, "/connections")) {
		    req.close();
		    return;
		}
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		Log.info("(web request from " + address + ") closing client "
			+ pair[1]);

		try {
		    core.closeClient(pair[1]);
		} catch (Exception ex) {
		    Log.warn("(web request from " + address
			    + ") failed closing client " + pair[1], ex);
		    //TODO eroare
		}

		redirectTo("/connections", req);
		break;
	    }

	    case "/block":
	    case "/block/": {
		if (!authProcess(req, "/connections")) {
		    req.close();
		    return;
		}
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		Log.info("(web request from " + address + ") blocking client "
			+ pair[1]);

		try {
		    core.blockClient(pair[1]);
		} catch (Exception ex) {
		    Log.warn("(web request from " + address
			    + ") failed blocking client " + pair[1], ex);
		}

		redirectTo("/connections", req);
		break;
	    }

	    case "/unblock":
	    case "/unblock/": {
		if (!authProcess(req, "/connections")) {
		    req.close();
		    return;
		}
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		Log.info("(web request from " + address + ") unblocking client "
			+ pair[1]);

		try {
		    core.unblockClient(pair[1]);
		} catch (Exception ex) {
		    Log.
			    warn("(web request from " + address
				    + ") failed unblocking client " + pair[1],
				    ex);
		}

		redirectTo("/connections", req);
		break;
	    }

	    case "/push":
	    case "/push/": {
		if (!authProcess(req, "/connections")) {
		    req.close();
		    return;
		}
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		Log.info("(web request from " + address + ") pushing to client "
			+ pair[1]);

		try {
		    core.pushClient(pair[1]);
		} catch (Exception ex) {
		    Log.
			    warn("(web request from " + address
				    + ") failed pushing to client " + pair[1],
				    ex);
		}

		redirectTo("/connections", req);
		break;
	    }

	    case "/login":
	    case "/login/":
		try {
		    Log.fine("web login attempt from " + address);
		    String redirectTo = null;
		    String id = null;
		    String pw = null;
		    String salt = null;

		    for (String entry : parsePostRequestQuery(req)) {
			final String[] pair = parsePair(entry);

			switch (pair[0]) {
			    case "id":
				id = pair[1];
				break;

			    case "password":
				pw = pair[1];
				break;

			    case "salt":
				salt = pair[1];
				break;

			    case "redirectTo":
				redirectTo = pair[1];
				break;

			    default:
				respond404(req);
				return;

			}
		    }

		    if (id == null || pw == null || salt == null) {
			respond404(req);
			return;
		    }

		    if (redirectTo != null) {
			if (checkCredentials(req, id, pw, salt)) {
			    redirectTo(redirectTo, req);
			} else {
			    writeAuthAfterFailed(req, redirectTo);
			}
		    } else {
			if (checkCredentials(req, id, pw, salt)) {
			    redirectTo("/connections", req);
			} else {
			    writeAuthAfterFailed(req, "/connections");
			}
		    }

		} catch (Exception ex) {
		    Log.debug("caught exception while processing login", ex);
		    respond404(req);
		}
		break;
	}

    }
}
