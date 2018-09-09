package andrei.dynamic.server;

import andrei.dynamic.common.MustResetConnectionException;
import andrei.dynamic.common.AddressInstance;
import andrei.dynamic.common.Log;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.common.MessageType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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

    private static final int SOCKET_TIMEOUT = 10000;
    private final CoreManager manager;
    private final Socket socket;
    private final AddressInstance clientControlAddress;
    private String authToken;
    private final OutputStream out;
    private final InputStream in;
    private boolean closing;
    private boolean updating;

    public ConnectionWrapper(final Socket socket, final CoreManager manager)
	    throws Exception {
	this.manager = manager;
	this.socket = socket;
	clientControlAddress = new AddressInstance(socket.
		getRemoteSocketAddress().
		toString(), socket.getPort());
	try {
	    socket.setTcpNoDelay(true);
	} catch (Exception ex) {
	    //nagle e on
	    Log.warn("could not disable nagle algorithm on socket for client "
		    + getStringAddress());
	}
	try {
	    socket.setSoTimeout(SOCKET_TIMEOUT);
	} catch (Exception e) {
	    Log.warn("could not enable socket reading timeout for client "
		    + getStringAddress());
	}

	out = getOutputStream(socket.getOutputStream());
	in = getInputStream(socket.getInputStream());

	closing = false;
    }

    public AddressInstance getClientControlAddress() {
	return clientControlAddress;
    }

    public final String getStringAddress() {
	return clientControlAddress.toString();
    }

    public boolean isUpdating() {
	return updating;
    }

    public void setUpdating(boolean updating) {
	this.updating = updating;
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
    }

    private void sendUpdateFileMessage(final String relative) throws Exception {

	byte[] msg = MessageFactory.newUpdatedFileMessage(relative);
	out.write(msg);
	out.flush();
    }

    private void sendCheckFileMessage(final String relative) throws Exception {
	byte[] msg = MessageFactory.newCheckFileMessage(relative);
	out.write(msg);
	out.flush();
    }

    public void startUploading(final String absolute) throws Exception {

	final BufferedInputStream fileInput = new BufferedInputStream(
		new FileInputStream(absolute));

	byte[] buff = new byte[4096];

	int read;
	while ((read = fileInput.read(buff, 0, 4096)) != -1) {
	    if (read != 0) {
		int check = read % 16;
		if (check == 0) {
		    out.write(MessageFactory.newTransferContinueMessage(read,
			    0));
		    out.write(buff, 0, read);
		} else {
		    out.write(MessageFactory.newTransferContinueMessage(read
			    + 16 - check, 16 - check));
		    out.write(buff, 0, read);
		    Arrays.fill(buff, 0, 16 - check, (byte) 0);
		    out.write(buff, 0, 16 - check);
		}
	    }
	}
	out.write(MessageFactory.newTransferEndMessage());

	out.flush();
	fileInput.close();
    }

    public void updateRemoteFile(final String relative, final String absolute) {
	if (Log.isTraceEnabled()) {
	    Log.trace("updating file " + relative + " on client " + authToken);
	}
	try {
	    sendUpdateFileMessage(relative);
	    startUploading(absolute);
	} catch (Exception ex) {
	    if (Log.isDebugEnabled()) {
		Log.debug("failed updating file " + relative + " on client "
			+ authToken, ex);
	    }
	}
    }

    public void deleteRemoteFile(final String relative) {
	if (Log.isTraceEnabled()) {
	    Log.trace("deleting file " + relative + " on client " + authToken);
	}
	try {
	    sendDeleteFileMessage(relative);
	} catch (Exception ex) {
	    if (Log.isDebugEnabled()) {
		Log.debug("failed deleting file " + relative + " on client "
			+ authToken, ex);
	    }
	}
    }

    public String testConnection() throws Exception {
	try {
	    sendTestMessage();
	} catch (Exception ex) {
	    throw new MustResetConnectionException("failed sending test message");
	}

	int type;
	try {
	    type = in.read();
	} catch (Exception ex) {
	    throw new MustResetConnectionException(ex.getMessage());
	}

	if (type == -1) {
	    throw new MustResetConnectionException("connection closed");
	}
	if (type != MessageType.TEST_MESSAGE_RSP.getCode()) {
	    throw new MustResetConnectionException("bad response");
	}

	int counter = 1;
	int dim = MessageFactory.STD_MSG_DIM;

	byte[] buff = new byte[dim];
	while (counter < dim) {
	    try {
		counter += in.read(buff, counter - 1, dim - counter);
	    } catch (Exception ex) {
		throw new MustResetConnectionException(ex.getMessage());
	    }
	}

	return new String(MessageFactory.trimPadding(buff));
    }

    public boolean checkRemoteFileMD5(final String relative, final byte[] md5)
	    throws Exception {

	sendCheckFileMessage(relative);

	if (in.read() != MessageType.CHECK_FILE_MESSAGE_RSP.getCode()) {
	    throw new MustResetConnectionException(
		    "unexpected message type in file check response");
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

	if (Log.isTraceEnabled()) {
	    Log.trace(DatatypeConverter.printHexBinary(MessageFactory.
		    trimPadding(Arrays.copyOfRange(buff,
			    MessageFactory.STD_MSG_DIM - 1, dim)))
		    + " local hash and " + DatatypeConverter.printHexBinary(md5)
		    + " remote hash");
	}
	return Arrays.equals(MessageFactory.trimPadding(Arrays.copyOfRange(buff,
		MessageFactory.STD_MSG_DIM - 1, dim)), md5);
    }

    private OutputStream getOutputStream(final OutputStream out)
	    throws Exception {
	if (manager.getSecretKey() == null) {
	    return new BufferedOutputStream(out);
	}

	final SecretKeySpec keySpec = new SecretKeySpec(manager.getSecretKey(),
		"AES");
	final IvParameterSpec ivSpec
		= new IvParameterSpec(manager.getSecretIv());
	final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

	return new CipherOutputStream(out, cipher);
    }

    private InputStream getInputStream(final InputStream in) throws
	    Exception {
	if (manager.getSecretKey() == null) {
	    return new BufferedInputStream(in);
	}

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
    }

    public void disconnect() {
	onClosing();
	try {
	    socket.shutdownInput();
	} catch (Exception ex) {
	    Log.fine("failed closing input for client " + clientControlAddress);
	}
	try {
	    socket.shutdownOutput();
	} catch (Exception ex) {
	    Log.fine("failed closing output for client " + clientControlAddress);
	}
	try {
	    socket.close();
	} catch (Exception ex) {
	    Log.fine("failed closing client " + clientControlAddress);
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

    @Override
    public String toString() {
	return getStringAddress();
    }

}
