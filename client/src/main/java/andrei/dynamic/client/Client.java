package andrei.dynamic.client;

import andrei.dynamic.common.Address;
import andrei.dynamic.common.DirectoryPathHelper;
import andrei.dynamic.common.MessageFactory;
import andrei.dynamic.common.MustResetConnectionException;
import andrei.dynamic.common.Log;
import andrei.dynamic.common.MessageType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Andrei
 */
public class Client {

    private final String localAddress;
    private final int localPort;
    private final Address controlAddress;
    private final Address dataAddress;
    private final String authToken;
    private final DirectoryPathHelper dir;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private final byte[] key;
    private final byte[] iv;
    private boolean keepWorking;
    private boolean reconnecting;

    public Client(final String localAddress, int localPort,
	    final Address controlAddress, final Address dataAddress,
	    final DirectoryPathHelper dir, final String authToken,
	    final String key) throws Exception {
	this.controlAddress = controlAddress;
	this.dataAddress = dataAddress;
	this.dir = dir;
	this.authToken = authToken;
	this.localAddress = localAddress;
	this.localPort = localPort;
	reconnecting = false;

	if (key == null || key.isEmpty()) {
	    this.key = null;
	    this.iv = null;
	} else {
	    final MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    final byte[] bytes = digest.digest(key.getBytes(
		    StandardCharsets.UTF_8));
	    this.key = Arrays.copyOf(bytes, 16);
	    iv = Arrays.copyOfRange(bytes, 16, 32);
	    Log.info("encryption activated");
	}

	keepWorking = true;
    }

    public void start(boolean keepAlive) throws Exception {

	Log.info("server " + controlAddress);

	while (keepWorking) {
	    try {
		initConnection();
	    } catch (MustResetConnectionException ex) {
		Log.fine("connection reset", ex);
	    } catch (ClientException ex) {
		if (reconnecting) {
		    Log.trace("connection failed", ex);
		} else {
		    Log.fine("connection failed", ex);
		}
		if (!keepAlive) {
		    throw new Exception("failed connecting to server");
		}
	    } catch (Exception ex) {
		Log.fatal("encountered exception", ex);
		if (!keepAlive) {
		    throw new Exception("failed connecting to server");
		}
	    } finally {
		if (socket != null) {
		    try {
			socket.close();
		    } catch (Exception ex) {
			Log.warn("failed closing tcp socket", ex);
		    }
		}
	    }
	    if (!keepWorking) {
		break;
	    }

	    if (reconnecting) {
		Log.trace("reconnecting...");
	    } else {
		Log.fine("reconnecting...");
		reconnecting = true;
	    }
	    Thread.sleep(3000);
	}

    }

    public void initConnection() throws Exception {

	try {
	    socket = new Socket();
	    socket.bind(new InetSocketAddress(localAddress, localPort));
	    socket.connect(new InetSocketAddress(controlAddress.getHost(),
		    controlAddress.getPort()), 1000);

	    if (!socket.isBound() || !socket.isConnected()) {
		throw new SocketException("control socket not bound");
	    }
	} catch (Exception ex) {
	    if (socket != null) {
		try {
		    socket.close();
		} catch (Exception ex2) {
		    Log.warn("failed closing tcp socket", ex2);
		}
	    }
	    throw new ClientException(ex.getMessage());
	}

	try {
	    try {
		socket.setTcpNoDelay(true);
	    } catch (Exception ex) {
		Log.warn("could not disable nagle algorithm");
	    }
	    try {
		socket.setSoTimeout(5000);
	    } catch (Exception e) {
		Log.warn("could not enable socket reading timeout");
	    }

	    out = getOutputStream(socket.getOutputStream());
	    in = getInputStream(socket.getInputStream());

	    final Address runtimeLocalAddress = new Address(socket.
		    getLocalAddress().toString(), socket.getLocalPort());

	    Log.fine("connected with address " + runtimeLocalAddress + " to "
		    + controlAddress);
	    Log.debug("authenticating using token " + authToken);
	    reconnecting = false;

	    while (keepWorking) {

		MessageFromServer message = null;

		try {

		    message = getMessage();

		    switch (message.getType()) {
			case CHECK_FILE_MESSAGE:
			    Log.trace("checking file " + new String(message.
				    getContent()).trim());
			    sendCheckFileMessageResponse(new String(
				    MessageFactory.trimPadding(message.
					    getContent())));
			    break;
			case UPDATE_FILE_MESSAGE:
			    Log.debug("updating file " + new String(message.
				    getContent()).trim());
			    downloadFile(new String(MessageFactory.trimPadding(
				    message.getContent())));
			    break;
			case DELETE_FILE_MESSAGE:
			    Log.debug("deleting file " + new String(message.
				    getContent()).trim());
			    deleteFile(new String(MessageFactory.trimPadding(
				    message.getContent())));
			    break;
			case TEST_MESSAGE:
			    sendTestResponse();
			    break;
			default:
			    throw new MustResetConnectionException(
				    "unexpected message type " + message.
					    getType());
		    }

		} catch (EOFException ex) {
		    //stop();
		    throw new MustResetConnectionException(ex.getMessage());
		} catch (Exception ex) {
		    throw ex;
		}

	    }
	} catch (Exception ex) {
	    throw ex;
	} finally {
	    if (socket != null) {
		try {
		    socket.close();
		} catch (Exception ex) {
		    Log.warn("failed closing tcp socket", ex);
		}
	    }

	    socket = null;
	    in = null;
	    out = null;

	}
    }

    private void sendTestResponse() throws Exception {
	out.write(MessageFactory.newTestMessageResponse(authToken));
	out.flush();
    }

    private void sendCheckFileMessageResponse(final String relative) throws
	    Exception {

	byte[] msg = MessageFactory.newCheckFileMessageResponse(relative,
		dir.getLocalFileMD5(dir.getAbsolutePath(relative)));
	out.write(msg);
	out.flush();
    }

    public void stop() {
	keepWorking = false;
    }

    public void disconnect() throws IOException { //TODO vezi ca ajunge close pe socket
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

    private MessageFromServer getMessage() throws Exception {

	int type = -2;

	while (type == -2) {
	    if (!keepWorking) {
		throw new TerminatedException("received shutdown signal");
	    }

	    try {
		type = in.read();
	    } catch (SocketTimeoutException ex) {
		type = -2;
	    }
	}

	if (type == -1) {
	    throw new EOFException();
	}

	int counter = 1;
	int dim = MessageFactory.dimForType(type);

	byte[] buff = new byte[dim];
	while (counter < dim) {
	    try {
		counter += in.read(buff, counter - 1, dim - counter);
	    } catch (SocketTimeoutException ex) {
		//nimic
	    }
	}

	return new MessageFromServer(type, buff);

    }

    private void downloadFile(final String relative) throws Exception {

	Path original = FileSystems.getDefault().getPath(dir.root(), relative);
	Path temp = dir.getTempFilePath(relative);

	Log.trace("starting data transfer");
	FileOutputStream writer = new FileOutputStream(temp.toString());

	final byte[] buff = new byte[4096];
	boolean streaming = true;

	while (streaming) {
	    final MessageFromServer message = getMessage();
	    int leftToRead;
	    int padding;

	    switch (message.getType()) {
		case TRANSFER_END:
		    streaming = false;
		    leftToRead = 0;
		    padding = 0;
		    break;
		case TRANSFER_CONTINUE:
		    leftToRead = (message.getContent()[3] & 0xff)
			    + ((message.getContent()[2] & 0xff) << 8)
			    + ((message.getContent()[1] & 0xff) << 16)
			    + ((message.getContent()[0] & 0xff) << 24);
		    padding = message.getContent()[4] & 0xff;
		    break;
		default:
		    throw new Exception("unexpected message type " + message.
			    getType() + " in file transfer");
	    }

	    while (leftToRead > 0) {
		int read = in.read(buff, 0, (4096 < leftToRead) ? 4096
			: leftToRead);
		if (read == -1) {
		    throw new MustResetConnectionException("connection closed");
		}
		leftToRead -= read;

		if (leftToRead < padding) {
		    if (leftToRead + read > padding) {
			writer.write(buff, 0, read - padding + leftToRead);
		    }
		} else {
		    writer.write(buff, 0, read);
		}
	    }
	}

	writer.flush();
	writer.close();

	if (!Files.exists(original.getParent())) {
	    Files.createDirectories(original.getParent());
	}
	Files.deleteIfExists(original);
	//(new File(temp.toString())).renameTo(new File(original.toString()));
	Files.move(temp, original, StandardCopyOption.REPLACE_EXISTING);
	if (Log.isTraceEnabled()) {
	    Log.trace("sucessfully downloaded file " + original.
		    toAbsolutePath());
	}
    }

    private boolean deleteFile(final String relative) throws Exception {
	Path absolute = FileSystems.getDefault().getPath(dir.root(), relative.
		trim());

	return Files.deleteIfExists(absolute);
    }

    private OutputStream getOutputStream(final OutputStream out)
	    throws Exception {
	if (key == null) {
	    return new BufferedOutputStream(out);
	}

	final SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
	final IvParameterSpec ivSpec = new IvParameterSpec(iv);
	final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

	return new CipherOutputStream(out, cipher);
    }

    private InputStream getInputStream(final InputStream in) throws
	    Exception {
	if (key == null) {
	    return new BufferedInputStream(in);
	}

	final SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
	final IvParameterSpec ivSpec = new IvParameterSpec(iv);
	final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
	cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

	return new CipherInputStream(in, cipher);
    }

    private static enum State {
	INIT,
	WAITING_UPDATE,
	DOWNLOADING,
	CLOSING,
	UPDATING,
	STARTING_DOWNLOAD
    }
}
