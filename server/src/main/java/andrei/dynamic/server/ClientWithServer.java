package andrei.dynamic.server;

import andrei.dynamic.common.MessageFactory;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 * @author Andrei
 */
public class ClientWithServer {

    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private FileGroup[] groups;

    public ClientWithServer(final Socket socket) {
	this.socket = socket;
    }

    public void start() throws Exception {
	try {
	    socket.setTcpNoDelay(true);
	} catch (Exception ex) {
	    //nagle e on
	}
	out = new DataOutputStream(socket.getOutputStream());
	in = new DataInputStream(socket.getInputStream());
    }

    public void sendTestMessage(final String path) {
	if (out == null){
	    System.err.println("esti nuuuuulll");
	}
	try {
	    byte[] msg = MessageFactory.createCheckFileMessage(path);
	    out.write(msg);
	    System.out.println("written " + path + " of length " + msg.length);
	} catch (Exception ex) {
	    System.out.println(" n-a mers pentru fisierul " + path);
	    ex.printStackTrace(System.out);
	}
    }

    private void sendInitMessage() {
	byte[] content = new byte[1];

	content[0] = 0x64; //mesaj de init
    }

    //TODO hash si equals
}
