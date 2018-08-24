package andrei.dynamic.server.http;

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

    public ActionsView(final CoreManager core) {
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

	switch (path) {
	    case "/disconnect":
	    case "/disconnect/":{
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		try {
		    core.closeClient(pair[1]);
		} catch (Exception ex) {
		    System.err.println("failed closing client " + pair[1]);
		    //TODO eroare
		}
		
		redirectTo("/connections", req);
		break;
	    }
	    
	    case "/block":
	    case "/block/":{
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		try {
		    core.blockClient(pair[1]);
		} catch (Exception ex) {
		    System.err.println("failed blocking client " + pair[1]);
		    //TODO eroare
		}
		
		redirectTo("/connections", req);
		break;
	    }
	    
	    case "/unblock":
	    case "/unblock/":{
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		try {
		    core.unblockClient(pair[1]);
		} catch (Exception ex) {
		    System.err.println("failed unblocking client " + pair[1]);
		    //TODO eroare
		}
		
		redirectTo("/connections", req);
		break;
	    }
	    
	    case "/push":
	    case "/push/":{
		final String[] pair = parsePair(req.getRequestURI().getQuery());

		if (pair == null || !"client".equals(pair[0]) || pair[1].
			length() > MessageFactory.STD_MSG_DIM - 1) {
		    respond404(req);
		    return;
		}

		try {
		    core.pushClient(pair[1]);
		} catch (Exception ex) {
		    System.err.println("failed pushing to client " + pair[1]);
		    //TODO eroare
		}
		
		redirectTo("/connections", req);
		break;
	    }

	}
    }

}
