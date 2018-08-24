package andrei.dynamic.server;

import andrei.dynamic.common.MustResetConnectionException;
import andrei.dynamic.server.jaxb.XmlFileGroup;
import andrei.dynamic.common.Address;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.common.MessageType;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Andrei
 */
public class ConnectionWrapper
	implements Comparable<ConnectionWrapper> {

    private static final int SOCKET_TIMEOUT = 5000;
    private final CoreManager manager;
    private final Socket socket;
    private final Address clientControlAddress;
    private String authToken;
    private CipherOutputStream out;
    private CipherInputStream in;
    private XmlFileGroup files;
    private boolean closing;

    public ConnectionWrapper(final Socket socket, int localDataPort,
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

    private void sendDeleteFileMessage(final String relative) throws Exception {
	byte[] msg = MessageFactory.newDeletedFileMessage(relative);
	out.write(msg);
	out.flush();
	System.out.println("written " + relative + " of length "
		+ msg.length);
    }

    private void sendTransferPaddingMessage(int padding) throws Exception {
	byte[] msg = MessageFactory.newTransferPaddingMessage(padding);
	out.write(msg);
	out.flush();
	System.out.println("written " + padding + " of length "
		+ msg.length);
    }

    private void sendUpdateFileMessage(final String relative) throws Exception {

	byte[] msg = MessageFactory.newUpdatedFileMessage(relative);
	out.write(msg);
	out.flush();
	System.out.println("written " + relative + " of length "
		+ msg.length);
    }

    private void sendCheckFileMessage(final String relative) throws Exception {
	byte[] msg = MessageFactory.newCheckFileMessage(relative);
	out.write(msg);
	out.flush();
    }

    public void startUploading(final String absolute) throws Exception {

	final Socket dataSocket = manager.acceptDataConnection();
	final CipherOutputStream dataOutput = getCipherOutputStream(dataSocket.
		getOutputStream());
	final BufferedInputStream fileInput = new BufferedInputStream(
		new FileInputStream(absolute));

	byte[] buff = new byte[1024 * 4];

	int read;
	int check = 0;
	while ((read = fileInput.read(buff)) != -1) {
	    dataOutput.write(buff, 0, read);
	    check = (check + read) % 16;
	    System.out.println("scriu");
	}
	if (check != 0) {
	    Arrays.fill(buff, 0, 16 - check, (byte) 0);
	    dataOutput.write(buff, 0, 16 - check);
	}
	dataOutput.close();
	fileInput.close();
	dataSocket.close();

	if (check == 0) {
	    sendTransferPaddingMessage(0);
	} else {
	    sendTransferPaddingMessage(16 - check);
	}

    }

    public void updateRemoteFile(final String relative, final String absolute) {
	try {
	    sendUpdateFileMessage(relative);
	    startUploading(absolute);
	} catch (Exception ex) {
	    System.out.println(" n-a mers update pentru fisierul " + relative);
	    ex.printStackTrace(System.out);
	}
    }

    public void deleteRemoteFile(final String relative) {
	try {
	    sendDeleteFileMessage(relative);
	} catch (Exception ex) {
	    System.out.println(" n-a mers delete pentru fisierul " + relative);
	    ex.printStackTrace(System.out);
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
	int dim = MessageFactory.STD_MSG_DIM;

	byte[] buff = new byte[dim];
	while (counter < dim) {
	    try {
		counter += in.read(buff, counter - 1, dim - counter);
	    } catch (Exception ex) {
		throw new MustResetConnectionException();
	    }
	}

	return new String(MessageFactory.trimPadding(buff));
    }

    public boolean checkRemoteFileMD5(final String relative, final byte[] md5)
	    throws Exception {

	sendCheckFileMessage(relative);

	if (in.read() != MessageType.CHECK_FILE_MESSAGE_RSP.getCode()) {
	    throw new MustResetConnectionException();
	}

	int counter = 1;
	int dim = MessageFactory.CHECK_FILE_MSG_RSP_DIM;
	byte[] buff = new byte[dim];
	while (counter < dim) {
	    counter += in.read(buff, counter - 1, dim - counter);
	}

	if (!new String(MessageFactory.trimPadding(Arrays.copyOf(buff,
		MessageFactory.STD_MSG_DIM - 1))).equals(relative)) {
	    return false;
	}

	/*System.out.println(DatatypeConverter.printHexBinary(MessageFactory.
		trimPadding(Arrays.copyOfRange(buff,
			MessageFactory.STD_MSG_DIM - 1, dim))) + " vs "
		+ DatatypeConverter.printHexBinary(md5));*/

	return Arrays.equals(MessageFactory.trimPadding(Arrays.copyOfRange(buff,
		MessageFactory.STD_MSG_DIM - 1, dim)), md5);
    }

    private CipherOutputStream getCipherOutputStream(final OutputStream out)
	    throws Exception {
	final SecretKeySpec keySpec = new SecretKeySpec(manager.getSecretKey(),
		"AES");
	final IvParameterSpec ivSpec
		= new IvParameterSpec(manager.getSecretIv());
	final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

	return new CipherOutputStream(out, cipher);
    }

    private CipherInputStream getCipherInputStream(final InputStream in) throws
	    Exception {
	final SecretKeySpec keySpec = new SecretKeySpec(manager.getSecretKey(),
		"AES");
	final IvParameterSpec ivSpec
		= new IvParameterSpec(manager.getSecretIv());
	final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

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
	final ConnectionWrapper other = (ConnectionWrapper) obj;
	if (!Objects.equals(this.clientControlAddress,
		other.clientControlAddress)) {
	    return false;
	}
	return true;
    }

    @Override
    public int compareTo(ConnectionWrapper other) {
	return clientControlAddress.compareTo(other.clientControlAddress);
    }

}
