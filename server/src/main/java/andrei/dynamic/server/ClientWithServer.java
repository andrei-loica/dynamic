package andrei.dynamic.server;

import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.common.Address;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.common.MessageType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.Key;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

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
    private String authToken;
    private final int localDataPort;
    private CipherOutputStream out;
    private CipherInputStream in;
    private XmlFileGroup files;
    private boolean closing;

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

	out = getCipherOutputStream(socket.getOutputStream());
	in = getCipherInputStream(socket.getInputStream());

	clientControlAddress = new Address(socket.getInetAddress().
		getHostAddress(),
		socket.getPort());
	this.localDataPort = localDataPort;
	closing = false;
	System.out.println(clientControlAddress.getHost() + " and port "
		+ clientControlAddress.getPort());
    }

    public Address getClientControlAddress() {
	return clientControlAddress;
    }

    public String getStringAddress() {
	return clientControlAddress.toString();
    }

    public void setAuthToken(final String token) {
	authToken = token;
    }

    public String getAuthToken() {
	return authToken;
    }

    public void sendCreateFileMessage(final String relative,
	    final String absolute) {
	try {
	    byte[] msg = MessageFactory.newCreatedFileMessage(relative);
	    out.write(msg);
	    out.flush();
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
	    out.flush();
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
	    out.flush();
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
		final CipherOutputStream dataOutput
		= getCipherOutputStream(dataSocket.getOutputStream());
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
	    if (in.read() != MessageType.TEST_MESSAGE_RSP.getCode()) {
		throw new MustResetConnectionException();
	    }
	} catch (MustResetConnectionException ex) {
	    throw ex;
	} catch (Exception ex) {
	    ex.printStackTrace(System.out);
	    return null;
	}

	int counter = 1;
	byte[] buff = new byte[MessageFactory.TEST_MSG_DIM];
	while (counter < MessageFactory.TEST_MSG_DIM) {
	    try {
		counter += in.read(buff, counter - 1, MessageFactory.TEST_MSG_DIM - counter);
	    } catch (Exception ex) {
		throw new MustResetConnectionException();
	    }
	}

	return new String(MessageFactory.trimPadding(buff));
    }

    private CipherOutputStream getCipherOutputStream(final OutputStream out)
	    throws Exception {
	final SecretKeySpec spec = new SecretKeySpec(manager.getSecretKey(),
		"AES");
	final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
	cipher.init(Cipher.ENCRYPT_MODE, spec);

	return new CipherOutputStream(out, cipher);
    }

    private CipherInputStream getCipherInputStream(final InputStream in) throws
	    Exception {
	final SecretKeySpec spec = new SecretKeySpec(manager.getSecretKey(),
		"AES");
	final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
	cipher.init(Cipher.DECRYPT_MODE, spec);

	return new CipherInputStream(in, cipher);
    }

    private void sendTestMessage() throws Exception {
	byte[] msg = MessageFactory.newTestMessage();
	out.write(msg);
	out.flush();
	//System.out.println("written test message");
    }

    public void disconnect() {
	onClosing();
	try {
	    socket.close();
	} catch (Exception ex) {
	    System.err.println("failed closing client " + clientControlAddress);
	}
    }

    public void onClosing() {
	closing = true;
    }

    public boolean isClosing() {
	return closing;
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
