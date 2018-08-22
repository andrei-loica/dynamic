package andrei.dynamic.client;

import andrei.dynamic.common.Address;
import andrei.dynamic.common.DirectoryManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 *
 * @author Andrei
 */
public class ClientConnection { //TODO rename Manager / ClientManager

    private final Address controlAddress; //TODO de scos
    private final Address dataAddress;
    private final byte[] key;
    private final DirectoryManager dir;
    private final ServerImage server;
    private State state;

    public ClientConnection(final Address controlAddress,
	    final Address dataAddress, final DirectoryManager workingDirectory,
	    final String authToken, final String key) throws Exception{

	this.controlAddress = controlAddress;
	this.dataAddress = dataAddress;
	dir = workingDirectory;
	
	final MessageDigest digest = MessageDigest.getInstance("SHA-256");
	this.key = Arrays.copyOf(digest.digest(key.getBytes(StandardCharsets.UTF_8)), 16);
	
	server = new ServerImage(this, controlAddress, dataAddress, dir,
		authToken);
	state = State.INITIALIZED;

    }

    /**
     * Starts a client connection bound to the given address. The given
     * directory will be treated as the working directory, so it should be
     * modified only through the Dynamic Config server.
     *
     * @param keepAlive if true, will not throw exception and set a reconnect
     * timer instead
     * @throws Exception if fails to connect
     */
    public void start(boolean keepAlive) throws Exception {

	if (state != State.INITIALIZED) {
	    throw new IllegalStateException("cannot start in " + state
		    + " state");
	}
	state = State.CONNECTING;
	System.out.println("opening new connection");

	while (state == State.CONNECTING) {
	    try {
		server.initConnection();
	    } catch (Exception ex) {
		if (!keepAlive) {
		    throw new Exception("failed connecting to server");
		}
		System.out.println("reconnecting...");
		continue;
	    }

	    state = State.WORKING;
	}

    }

    public void stop() {
	state = State.FINAL;
	server.stop();

	//graceful stop
    }
    
    public byte[] getSecretKey(){
	return key;
    }

    private static enum State {
	INITIALIZED,
	CONNECTING,
	//RECONNECTING,
	WORKING,
	FINAL
    }

}
