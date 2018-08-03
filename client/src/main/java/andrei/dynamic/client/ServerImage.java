package andrei.dynamic.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 *
 * @author Andrei
 */
public class ServerImage {

    private final Address address;
    private final ClientConnection parent;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public ServerImage(final ClientConnection parent, final Address address) {
	this.parent = parent;
	this.address = address;
    }

    public void initConnection() throws UnknownHostException, IOException {

	socket = new Socket(address.getHost(), address.getPort());
	socket.setTcpNoDelay(true);
	if (!socket.isBound()) {
	    throw new SocketException("socket not bound in server image");
	}

	out = new DataOutputStream(socket.getOutputStream());
	in = new DataInputStream(socket.getInputStream());

	while (true) {
	    int counter = 0;
	    byte[] buff = new byte[130];
	    while (counter < 129) {
		counter += in.read(buff, counter, 129 - counter);
	    }

	    System.out.println(new String(buff));
	}
    }

    public void disconnect() throws IOException {
	final StringBuilder exceptionMessage = new StringBuilder();
	try {
	    out.close();
	} catch (Exception ex) {
	    exceptionMessage.append(ex.getMessage()).append(" ; ");
	    throw ex;
	} finally {
	    try {
		in.close();
	    } catch (Exception ex) {
		exceptionMessage.append(ex.getMessage()).append(" ; ");
		throw ex;
	    } finally {
		try {
		    socket.close();
		} catch (Exception ex) {
		    exceptionMessage.append(ex.getMessage()).append(" ; ");
		    throw ex;
		}
	    }
	    //TODO fa ceva cu mesajele de exceptie
	}
    }

}
