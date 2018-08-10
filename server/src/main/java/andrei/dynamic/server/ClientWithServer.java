package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.FileGroup;
import andrei.dynamic.common.Address;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.common.MessageType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.Socket;
import java.util.Objects;

/**
 *
 * @author Andrei
 */
public class ClientWithServer
	implements Comparable<ClientWithServer> {

    private static final int SOCKET_TIMEOUT = 5000;
    private final CoreManager manager;
    private final Socket socket;
    private final Address clientControlAddress;
    private final int localDataPort;
    private DataOutputStream out;
    private DataInputStream in;
    private FileGroup files;

    public ClientWithServer(final Socket socket, int localDataPort,
	    final CoreManager manager) throws Exception {
	this.manager = manager;
	this.socket = socket;
	try {
	    socket.setTcpNoDelay(true);
	} catch (Exception ex) {
	    //nagle e on
	    System.err.println("could not disable nagle algorithm");
	}
	try {
	    socket.setSoTimeout(SOCKET_TIMEOUT);
	} catch (Exception e) {
	    System.err.println("could not enable socket reading timeout");
	}

	out = new DataOutputStream(socket.getOutputStream());
	in = new DataInputStream(socket.getInputStream());

	clientControlAddress = new Address(socket.getInetAddress().
		getHostAddress(),
		socket.getPort());
	this.localDataPort = localDataPort;
	System.out.println(clientControlAddress.getHost() + " and port "
		+ clientControlAddress.getPort());
    }

    public Address getClientControlAddress() {
	return clientControlAddress;
    }

    public String getStringAddress() {
	return clientControlAddress.toString();
    }

    public void sendCreateFileMessage(final String relative,
	    final String absolute) {
	try {
	    byte[] msg = MessageFactory.newCreatedFileMessage(relative);
	    out.write(msg);
	    System.out.println("written " + relative + " of length "
		    + msg.length);
	    startUploading(absolute);
	} catch (Exception ex) {
	    System.out.println(" n-a mers pentru fisierul " + relative);
	    ex.printStackTrace(System.out);
	}
    }

    public void sendDeleteFileMessage(final String relative,
	    final String absolute) {
	try {
	    byte[] msg = MessageFactory.newDeletedFileMessage(relative);
	    out.write(msg);
	    System.out.println("written " + relative + " of length "
		    + msg.length);
	} catch (Exception ex) {
	    System.out.println(" n-a mers pentru fisierul " + relative);
	    ex.printStackTrace(System.out);
	}
    }

    public void sendModifyFileMessage(final String relative,
	    final String absolute) {
	try {
	    byte[] msg = MessageFactory.newModifiedFileMessage(relative);
	    out.write(msg);
	    System.out.println("written " + relative + " of length "
		    + msg.length);
	    startUploading(absolute);
	} catch (Exception ex) {
	    System.out.println(" n-a mers pentru fisierul " + relative);
	    ex.printStackTrace(System.out);
	}
    }

    public void startUploading(final String absolute) throws Exception {

	try (final Socket dataSocket = manager.acceptDataConnection();
		final BufferedOutputStream dataOutput
		= new BufferedOutputStream(
			dataSocket.getOutputStream());
		final BufferedInputStream fileInput = new BufferedInputStream(
			new FileInputStream(absolute));) {

	    byte[] buff = new byte[1024 * 4];

	    int read = 0;
	    while ((read = fileInput.read(buff)) != -1) {
		dataOutput.write(buff, 0, read);
		System.out.println("scriu");
	    }
	}
    }

    public String testConnection() throws MustResetConnectionException {
	try {
	    sendTestMessage();
	} catch (Exception ex) {
	    System.err.println("failed test connection");
	    ex.printStackTrace(System.err);
	    return null;
	}

	try {
	    if (in.readByte() != (byte) MessageType.TEST_MESSAGE_RSP.getCode()) {
		throw new MustResetConnectionException();
	    }
	} catch (MustResetConnectionException ex) {
	    throw ex;
	} catch (Exception ex) {
	    return null;
	}

	int counter = 0;
	byte[] buff = new byte[130];
	while (counter < 128) {
	    try {
		counter += in.read(buff, counter, 128 - counter);
	    } catch (Exception ex) {
		throw new MustResetConnectionException();
	    }
	}

	return new String(MessageFactory.trimPadding(buff));
    }

    private void sendTestMessage() throws Exception {
	byte[] msg = MessageFactory.newTestMessage();
	out.write(msg);
	//System.out.println("written test message");
    }

    public void disconnect() {
	try {
	    socket.close();
	} catch (Exception ex) {
	    System.err.println("failed closing client " + clientControlAddress);
	}
    }

    //TODO hash si equals
    @Override
    public int hashCode() {
	int hash = 5;
	hash = 79 * hash + Objects.hashCode(this.clientControlAddress);
	return hash;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ClientWithServer other = (ClientWithServer) obj;
	if (!Objects.equals(this.clientControlAddress,
		other.clientControlAddress)) {
	    return false;
	}
	return true;
    }

    @Override
    public int compareTo(ClientWithServer other) {
	return clientControlAddress.compareTo(other.clientControlAddress);
    }

}
